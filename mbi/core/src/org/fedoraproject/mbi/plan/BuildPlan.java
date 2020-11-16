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
package org.fedoraproject.mbi.plan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.fedoraproject.mbi.Reactor;
import org.fedoraproject.mbi.model.Execution;
import org.fedoraproject.mbi.model.ModuleDescriptor;

/**
 * @author Mikolaj Izdebski
 */
public class BuildPlan
{
    private final Reactor reactor;

    private final Set<ModuleDescriptor> processing = new LinkedHashSet<>();

    private final Map<ModuleDescriptor, List<BuildStep>> cache = new LinkedHashMap<>();

    private final List<BuildStep> steps = new ArrayList<>();

    public BuildPlan( Reactor reactor )
    {
        this.reactor = reactor;
    }

    public Reactor getReactor()
    {
        return reactor;
    }

    public void computeBuildPlan( Collection<ModuleDescriptor> modules )
        throws Exception
    {
        for ( ModuleDescriptor module : modules )
        {
            generateBuildSteps( module );
        }
    }

    private String formatCycle( ModuleDescriptor module )
    {
        Queue<ModuleDescriptor> cycle = new ArrayDeque<>( processing );
        while ( cycle.peek() != module )
        {
            cycle.remove();
        }
        cycle.add( module );
        return cycle.stream().map( ModuleDescriptor::getName ).collect( Collectors.joining( " -> " ) );
    }

    private void generateBuildSteps( ModuleDescriptor module )
        throws Exception
    {
        if ( cache.containsKey( module ) )
        {
            return;
        }
        if ( !processing.add( module ) )
        {
            throw new IllegalStateException( "Dependency cycle: " + formatCycle( module ) );
        }
        List<BuildStep> depSteps = new ArrayList<>();
        for ( String dependencyName : module.getDependencies() )
        {
            ModuleDescriptor dependency = reactor.getModule( dependencyName );
            generateBuildSteps( dependency );
            depSteps.addAll( cache.get( dependency ) );
        }
        cache.put( module, depSteps );
        for ( Execution execution : module.getExecutions() )
        {
            ModuleDescriptor toolModule = reactor.tryGetModule( "mbi-" + execution.getToolName() );
            BuildStep step = new BuildStep( reactor, module, toolModule, execution );
            steps.add( step );
            if ( toolModule != null )
            {
                generateBuildSteps( toolModule );
                step.addDependencies( cache.get( toolModule ) );
            }
            step.addDependencies( depSteps );
            depSteps.clear();
            depSteps.add( step );
        }
        processing.remove( module );
    }

    public List<BuildStep> getSteps()
    {
        return Collections.unmodifiableList( steps );
    }
}
