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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * @author Mikolaj Izdebski
 */
public abstract class Command
{
    private Queue<String> arguments;

    private final Set<String> flags = new LinkedHashSet<>();

    private final Map<String, String> options = new LinkedHashMap<>();

    public Collection<String> getArgs()
    {
        return arguments;
    }

    public boolean hasFlag( String flag )
    {
        return flags.contains( flag );
    }

    public String getOption( String key, Object defaultValue )
    {
        return options.getOrDefault( key, defaultValue.toString() );
    }

    public void setArgs( String[] args )
    {
        arguments = new ArrayDeque<>( Arrays.asList( args ) );
        arguments.remove(); // Command name
        while ( !arguments.isEmpty() && arguments.peek().startsWith( "-" ) )
        {
            String option = arguments.remove().substring( 1 );
            if ( option.contains( "=" ) )
            {
                String[] keyVal = option.split( "=", 2 );
                options.put( keyVal[0], keyVal[1] );
            }
            else
            {
                flags.add( option );
            }
        }
    }

    public abstract void run()
        throws Exception;

    protected static void print( String fmt, Object... args )
    {
        System.out.printf( fmt, args );
        System.out.println();
    }
}
