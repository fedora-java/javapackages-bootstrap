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

import java.util.LinkedHashMap;
import java.util.Map;

import org.fedoraproject.mbi.build.BuildCommand;
import org.fedoraproject.mbi.dist.DistCommand;

/**
 * @author Mikolaj Izdebski
 */
public class Main
{
    private static final Map<String, Class<? extends Command>> COMMANDS = new LinkedHashMap<>();
    static
    {
        COMMANDS.put( "build", BuildCommand.class );
        COMMANDS.put( "dist", DistCommand.class );
        COMMANDS.put( "graph", GraphCommand.class );
    }

    public static void main( String[] args )
        throws Exception
    {
        if ( args.length == 0 || !COMMANDS.containsKey( args[0] ) )
        {
            System.err.println( "Usage: mbi <command>" );
            System.err.println( "Where <command> is one of:" );
            System.err.println( "  - build - build and assemble stage 3" );
            System.err.println( "  - dist - create stage 3 distribution" );
            System.err.println( "  - graph - output project dependency graph" );
            System.exit( 1 );
        }

        Command command = COMMANDS.get( args[0] ).getConstructor().newInstance();
        command.setArgs( args );
        command.run();
    }
}