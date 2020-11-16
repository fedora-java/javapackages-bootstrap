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

import java.util.Collection;

import org.fedoraproject.mbi.Command;
import org.fedoraproject.mbi.Reactor;
import org.fedoraproject.mbi.model.ModuleDescriptor;
import org.fedoraproject.mbi.plan.BuildPlan;

/**
 * @author Mikolaj Izdebski
 */
public class BuildCommand
    extends Command
{
    @Override
    public void run()
        throws Exception
    {
        boolean incremental = hasFlag( "incremental" ) || hasFlag( "i" );
        boolean parallel = hasFlag( "parallel" ) || hasFlag( "j" );
        boolean keepGoing = hasFlag( "keepGoing" ) || hasFlag( "k" );
        Reactor reactor = Reactor.defaultReactor();
        Collection<ModuleDescriptor> modules = reactor.getModules( getArgs() );
        BuildPlan plan = new BuildPlan( reactor );
        plan.computeBuildPlan( modules );
        BuildExecutor executor = new BuildExecutor( incremental, parallel, keepGoing );
        executor.executeBuildPlan( plan );
    }
}
