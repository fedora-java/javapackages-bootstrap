/*-
 * Copyright (c) 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fedoraproject.mbi.build;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.fedoraproject.mbi.Util;
import org.fedoraproject.mbi.model.ModuleDescriptor;
import org.fedoraproject.mbi.plan.BuildPlan;
import org.fedoraproject.mbi.plan.BuildStep;
import org.fedoraproject.mbi.tool.ToolUtils;

/**
 * @author Mikolaj Izdebski
 */
class BuildExecutor
{
    private final boolean incremental;

    private final boolean parallel;

    private final boolean keepGoing;

    /** Steps that are not completable due to incomplete dependencies. */
    private final Set<BuildStep> blockedSteps;

    /** Incomplete steps which no incomplete dependencies. */
    private final Queue<BuildStep> completableSteps;

    /** Steps being under active competion. */
    private int completingSteps;

    /** Steps that have been successfully completed. */
    private final Set<BuildStep> completeSteps;

    /** Steps which completion failed with exception. */
    private final Map<BuildStep, Throwable> failedSteps;

    public BuildExecutor( boolean incremental, boolean parallel, boolean keepGoing )
    {
        this.incremental = incremental;
        this.parallel = parallel;
        this.keepGoing = keepGoing;
        blockedSteps = new LinkedHashSet<>();
        completableSteps = new LinkedList<>();
        completeSteps = new LinkedHashSet<>();
        failedSteps = new LinkedHashMap<>();
    }

    /**
     * Take a single completable step from the queue, blocking if necessary.
     * 
     * @return completable build step, or {@code null} if all steps have already been finished
     */
    private synchronized BuildStep takeCompletableStep()
    {
        while ( ( !completableSteps.isEmpty() || ( !blockedSteps.isEmpty() && completingSteps > 0 ) )
            && ( keepGoing || failedSteps.isEmpty() ) )
        {
            if ( !completableSteps.isEmpty() )
            {
                var step = completableSteps.remove();
                completingSteps++;
                System.err.println( step.getModule().getName() + ": " + step.getToolName() );
                return step;
            }
            try
            {
                wait();
            }
            catch ( InterruptedException e )
            {
                throw new RuntimeException( e );
            }
        }
        return null;
    }

    /**
     * Attempt to complete build step.
     * 
     * @param step build step to complete
     * @throws Exception if build step failed
     */
    private void completeStep( BuildStep step )
        throws Exception
    {
        if ( step.isInitial() )
        {
            Util.delete( step.getReactor().getTargetDir( step.getModule() ) );
        }
        ToolUtils.runToolOnProject( step.getReactor(), step.getToolModule(), step.getModule(), step.getExecution() );
        if ( step.isFinal() )
        {
            Path stampPath = step.getReactor().getTargetDir( step.getModule() ).resolve( "stamp" );
            Files.createFile( stampPath );
        }
    }

    private synchronized void markStepFailed( BuildStep step, Throwable throwable )
    {
        completingSteps--;
        if ( throwable instanceof InvocationTargetException && throwable.getCause() != null )
        {
            throwable = throwable.getCause();
        }
        failedSteps.put( step, throwable );
        notifyAll();
        throwable.printStackTrace();
    }

    private synchronized void markStepComplete( BuildStep step )
    {
        completingSteps--;
        completeSteps.add( step );
        for ( var it = blockedSteps.iterator(); it.hasNext(); )
        {
            var s = it.next();
            if ( completeSteps.containsAll( s.getDependencies() ) )
            {
                it.remove();
                completableSteps.add( s );
            }
        }
        notifyAll();
    }

    /**
     * Complete completable steps in the queue, one at time.
     */
    private void stepCompletionLoop()
    {
        for ( BuildStep step = takeCompletableStep(); step != null; step = takeCompletableStep() )
        {
            try
            {
                completeStep( step );
            }
            catch ( Throwable throwable )
            {
                markStepFailed( step, throwable );
                continue;
            }
            markStepComplete( step );
        }
    }

    private void completeStamped( BuildPlan plan )
    {
        Map<ModuleDescriptor, List<BuildStep>> moduleSteps = new LinkedHashMap<>();

        for ( BuildStep step : plan.getSteps() )
        {
            ModuleDescriptor module = step.getModule();
            moduleSteps.computeIfAbsent( module, x -> new ArrayList<>() ).add( step );
            moduleSteps.put( module, moduleSteps.remove( module ) );
        }

        moduleSteps.forEach( ( module, steps ) ->
        {
            Path stampPath = plan.getReactor().getTargetDir( module ).resolve( "stamp" );
            if ( Files.isRegularFile( stampPath, LinkOption.NOFOLLOW_LINKS ) )
            {
                List<BuildStep> deps = steps.stream() //
                                            .map( modStep -> modStep.getDependencies() ) //
                                            .flatMap( List::stream ).filter( BuildStep::isFinal ) //
                                            .collect( Collectors.toList() );
                if ( completeSteps.containsAll( deps ) )
                {
                    completeSteps.addAll( steps );
                }
            }
        } );
    }

    public void executeBuildPlan( BuildPlan plan )
    {
        if ( incremental )
        {
            completeStamped( plan );
        }

        for ( var step : plan.getSteps() )
        {
            if ( !completeSteps.contains( step ) )
            {
                if ( completeSteps.containsAll( step.getDependencies() ) )
                {
                    completableSteps.add( step );
                }
                else
                {
                    blockedSteps.add( step );
                }
            }
        }

        try
        {
            Thread[] threads = new Thread[parallel ? Runtime.getRuntime().availableProcessors() : 0];
            for ( int i = 0; i < threads.length; i++ )
            {
                threads[i] = new Thread( this::stepCompletionLoop );
                threads[i].start();
            }
            stepCompletionLoop();
            for ( var thread : threads )
            {
                thread.join();
            }
        }
        catch ( InterruptedException e )
        {
            throw new RuntimeException( e );
        }

        for ( var step : failedSteps.entrySet() )
        {
            System.err.println( "Failed to execute tool " + step.getKey().getToolName() + //
                " on module " + step.getKey().getModule().getName() + ": " + step.getValue().getMessage() );
        }
        if ( failedSteps.isEmpty() )
        {
            System.err.println( "BUILD SUCCESS" );
        }
        else
        {
            System.err.println( "BUILD FAILURE" );
            System.exit( 1 );
        }
    }
}
