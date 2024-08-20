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

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collection;

import org.fedoraproject.mbi.Reactor;
import org.fedoraproject.mbi.model.Instruction;
import org.fedoraproject.mbi.model.ModuleDescriptor;
import org.fedoraproject.mbi.model.ProjectDescriptor;

/**
 * @author Mikolaj Izdebski
 */
public abstract class Tool
{
    private Reactor reactor;

    private ModuleDescriptor module;

    public Reactor getReactor()
    {
        return reactor;
    }

    public void setReactor( Reactor reactor )
    {
        this.reactor = reactor;
    }

    public ModuleDescriptor getModule()
    {
        return module;
    }

    public void setModule( ModuleDescriptor module )
    {
        this.module = module;
    }

    public void initialize()
        throws Exception
    {
    }

    public void executeInstruction( Instruction instruction )
        throws ReflectiveOperationException
    {
        Method method = getClass().getMethod( instruction.getMethodName(), String.class );
        if ( !method.isAnnotationPresent( org.fedoraproject.mbi.tool.Instruction.class ) )
        {
            throw new RuntimeException( "Method " + method.getName() + " class " + getClass().getName()
                + " is not instruction" );
        }
        method.invoke( this, instruction.getArgument() );
    }

    public abstract void execute()
        throws Exception;

    public ProjectDescriptor getProject()
    {
        return getReactor().getProject( getModule().getProjectName() );
    }

    protected Path getSourceRootDir()
    {
        return getReactor().getSourceRootDir( getModule() );
    }

    protected Path getTargetDir()
    {
        return getReactor().getTargetDir( getModule() );
    }

    protected Path getGeneratedSourcesDir()
    {
        return getReactor().getGeneratedSourcesDir( getModule() );
    }

    protected Path getClassesDir()
    {
        return getReactor().getClassesDir( getModule() );
    }

    protected Collection<Path> getClassPath()
    {
        return getReactor().getClassPath( getModule() );
    }
}
