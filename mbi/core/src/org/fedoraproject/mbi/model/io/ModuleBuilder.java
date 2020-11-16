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
package org.fedoraproject.mbi.model.io;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.fedoraproject.mbi.model.Execution;
import org.fedoraproject.mbi.model.ModuleDescriptor;

/**
 * @author Mikolaj Izdebski
 */
class ModuleBuilder
{
    private String name;

    private final String projectName;

    private Path projectSubDir;

    private final Set<String> dependencies = new LinkedHashSet<>();

    private List<Execution> executions;

    public ModuleBuilder( String projectName )
    {
        this.projectName = projectName;
        this.name = projectName;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public void setProjectSubDir( Path projectSubDir )
    {
        this.projectSubDir = projectSubDir;
    }

    public void addDependency( String dep )
    {
        dependencies.add( dep );
    }

    public void setExecutions( List<Execution> executions )
    {
        this.executions = executions;
    }

    public ModuleDescriptor build()
    {
        return new ModuleDescriptor( name, projectName, projectSubDir, dependencies, executions );
    }
}
