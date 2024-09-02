/*-
 * Copyright (c) 2023 Red Hat, Inc.
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
package org.fedoraproject.mbi.licensing;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.fedoraproject.mbi.Command;
import org.fedoraproject.mbi.Reactor;

/**
 * @author Mikolaj Izdebski
 */
public class LicensingCommand
    extends Command
{
    @Override
    public void run()
        throws Exception
    {
        Reactor reactor = Reactor.defaultReactor();

        Map<String, String> tags = new TreeMap<>();
        for ( var project : reactor.getProjects() )
        {
            tags.put( project.getName(), project.getLicensing().getTag() );
        }
        String combined = "(" + tags.values().stream().collect( Collectors.joining( ") AND (" ) ) + ")";

        print( "# Licensing breakdown: " );
        for ( Entry<String, String> entry : tags.entrySet() )
        {
            print( "#    " + entry.getKey() + " has license: " + new SPDX( entry.getValue() ) );
        }
        print( "# Therefore combined license is:" );
        print( "License:        " + new SPDX( combined ) );
    }
}
