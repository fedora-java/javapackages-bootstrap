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
package org.fedoraproject.mbi.model;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Mikolaj Izdebski
 */
public class ModuleDescriptor
{
    private final String name;

    private final String projectName;

    private final Path projectSubDir;

    private final Set<String> dependencies;

    private final List<Execution> executions;

    public ModuleDescriptor( String name, String projectName, Path projectSubDir, Set<String> dependencies,
                             List<Execution> executions )
    {
        this.name = name;
        this.projectName = projectName;
        this.projectSubDir = projectSubDir;
        this.dependencies = Collections.unmodifiableSet( dependencies );
        this.executions = Collections.unmodifiableList( executions );
    }

    public String getName()
    {
        return name;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public Path getProjectSubDir()
    {
        return projectSubDir;
    }

    public Set<String> getDependencies()
    {
        return dependencies;
    }

    public List<Execution> getExecutions()
    {
        return executions;
    }
}
