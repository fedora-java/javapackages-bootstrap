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
package org.fedoraproject.mbi.tool;

import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.fedoraproject.mbi.Reactor;
import org.fedoraproject.mbi.model.Execution;
import org.fedoraproject.mbi.model.Instruction;
import org.fedoraproject.mbi.model.ModuleDescriptor;

/**
 * @author Mikolaj Izdebski
 */
public class ToolUtils
{
    private static final ReadWriteLock UNSAFE_TOOL_LOCK = new ReentrantReadWriteLock();

    private static final Lock SHARED_LOCK = UNSAFE_TOOL_LOCK.readLock();

    private static final Lock EXCLUSIVE_LOCK = UNSAFE_TOOL_LOCK.writeLock();

    public static URLClassLoader newClassLoader( Reactor reactor, ModuleDescriptor... modules )
    {
        try
        {
            List<URL> urls = new ArrayList<>();
            for ( ModuleDescriptor module : modules )
            {
                if ( module != null )
                {
                    urls.add( reactor.getClassesDir( module ).toUri().toURL() );
                    for ( Path path : reactor.getClassPath( module ) )
                    {
                        urls.add( path.toUri().toURL() );
                    }
                }
            }
            return new URLClassLoader( urls.toArray( new URL[] {} ), ToolUtils.class.getClassLoader() );
        }
        catch ( MalformedURLException e )
        {
            throw new RuntimeException( e );
        }
    }

    public static void runToolOnProject( Reactor reactor, ModuleDescriptor toolModule, ModuleDescriptor module,
                                         Execution exec )
        throws Exception
    {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try ( URLClassLoader cl = ToolUtils.newClassLoader( reactor, toolModule, module ) )
        {
            Thread.currentThread().setContextClassLoader( cl );
            var entryClassName = Tool.class.getPackage().getName() + "." + exec.getToolName() + "."
                + exec.getToolName().substring( 0, 1 ).toUpperCase() + exec.getToolName().substring( 1 ) + "Tool";
            Class<?> toolClass = cl.loadClass( entryClassName );
            boolean threadUnsafe = toolClass.isAnnotationPresent( ThreadUnsafe.class );
            Path logFile = reactor.getTargetDir( module ).resolve( exec.getToolName() + ".log" );
            Files.createDirectories( logFile.getParent() );
            Lock lock = threadUnsafe ? EXCLUSIVE_LOCK : SHARED_LOCK;
            PrintStream out = null;
            PrintStream err = null;
            try ( PrintStream log = threadUnsafe ? //
                            new PrintStream( Files.newOutputStream( logFile, //
                                                                    StandardOpenOption.CREATE,
                                                                    StandardOpenOption.APPEND ) )
                            : null )
            {
                lock.lock();
                System.err.println( module.getName() + ": " + exec.getToolName() );
                if ( threadUnsafe )
                {
                    out = System.out;
                    err = System.err;
                    System.setOut( log );
                    System.setErr( log );
                }
                Tool tool = (Tool) toolClass.getConstructor().newInstance();
                tool.setReactor( reactor );
                tool.setModule( module );
                tool.initialize();
                for ( Instruction instruction : exec.getInstructions() )
                {
                    tool.executeInstruction( instruction );
                }
                tool.execute();
            }
            catch ( Exception e )
            {
                if ( threadUnsafe )
                {
                    System.err.print( Files.readString( logFile ) );
                }
                throw e;
            }
            finally
            {
                if ( threadUnsafe )
                {
                    System.setOut( out );
                    System.setErr( err );
                }
                lock.unlock();
            }
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( oldCl );
        }
    }
}
