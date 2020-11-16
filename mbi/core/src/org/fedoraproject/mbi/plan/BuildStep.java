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

import java.util.ArrayList;
import java.util.List;

import org.fedoraproject.mbi.Reactor;
import org.fedoraproject.mbi.model.Execution;
import org.fedoraproject.mbi.model.Instruction;
import org.fedoraproject.mbi.model.ModuleDescriptor;

/**
 * @author Mikolaj Izdebski
 */
public class BuildStep
{
    private final Reactor reactor;

    private final ModuleDescriptor module;

    private final ModuleDescriptor toolModule;

    private final Execution execution;

    private final List<BuildStep> dependencies = new ArrayList<>();

    public BuildStep( Reactor reactor, ModuleDescriptor module, ModuleDescriptor toolModule, Execution execution )
    {
        this.reactor = reactor;
        this.module = module;
        this.toolModule = toolModule;
        this.execution = execution;
    }

    public Reactor getReactor()
    {
        return reactor;
    }

    public String getToolName()
    {
        return getExecution().getToolName();
    }

    public ModuleDescriptor getModule()
    {
        return module;
    }

    public ModuleDescriptor getToolModule()
    {
        return toolModule;
    }

    public Execution getExecution()
    {
        return execution;
    }

    public List<Instruction> getInstructions()
    {
        return getExecution().getInstructions();
    }

    public void addDependencies( List<BuildStep> deps )
    {
        dependencies.addAll( deps );
    }

    public List<BuildStep> getDependencies()
    {
        return dependencies;
    }

    public boolean isInitial()
    {
        return execution == module.getExecutions().iterator().next();
    }

    public boolean isFinal()
    {
        return execution == module.getExecutions().listIterator( module.getExecutions().size() ).previous();
    }
}