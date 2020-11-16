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
package org.fedoraproject.mbi;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.fedoraproject.mbi.model.ModuleDescriptor;
import org.fedoraproject.mbi.plan.BuildPlan;
import org.fedoraproject.mbi.plan.BuildStep;

/**
 * @author Mikolaj Izdebski
 */
public class GraphCommand
    extends Command
{
    @Override
    public void run()
        throws Exception
    {
        Reactor reactor = Reactor.defaultReactor();
        Collection<ModuleDescriptor> modules = reactor.getModules();
        BuildPlan plan = new BuildPlan( reactor );
        plan.computeBuildPlan( modules );

        Map<BuildStep, String> id = new LinkedHashMap<>();
        for ( BuildStep step : plan.getSteps() )
        {
            id.put( step, "s" + id.size() );
        }

        print( "digraph deps {" );
        print( "rankdir=\"BT\"" );
        print( "node [shape=box3d, style=filled, fillcolor=khaki1]" );

        for ( BuildStep step : plan.getSteps() )
        {
            String module = step.getModule().getName();
            String tool = step.getToolName();

            String label = String.format( "%s\\n(%s)", module, tool );

            StringBuilder tooltip = new StringBuilder();
            tooltip.append( "Execute tool " ).append( tool ).append( "\\n" );
            tooltip.append( "on project " ).append( module ).append( "\\n" );

            print( "%s [label=\"%s\", tooltip=\"%s\"]", id.get( step ), label, tooltip );
            for ( BuildStep dep : step.getDependencies() )
            {
                print( "%s -> %s", id.get( step ), id.get( dep ) );
            }
        }

        print( "}" );
    }
}
