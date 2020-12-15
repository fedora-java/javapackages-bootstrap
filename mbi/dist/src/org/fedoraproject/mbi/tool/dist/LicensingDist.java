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
package org.fedoraproject.mbi.tool.dist;

import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import org.fedoraproject.mbi.Reactor;
import org.fedoraproject.mbi.dist.DistRequest;

/**
 * @author Mikolaj Izdebski
 */
public class LicensingDist
{
    private final Reactor reactor;

    private final DistRequest dist;

    public LicensingDist( DistRequest dist )
    {
        this.reactor = dist.getReactor();
        this.dist = dist;
    }

    public void doDist()
        throws Exception
    {
        var licensesDir = dist.getInstallRoot().resolve( dist.getLicensesPath() );
        Files.createDirectories( licensesDir );

        for ( var project : reactor.getProjects() )
        {
            var licensing = project.getLicensing();
            for ( var file : licensing.getFiles() )
            {
                var path = reactor.getProjectDir( project ).resolve( file );
                if ( !Files.isRegularFile( path ) )
                {
                    throw new RuntimeException( "License file for " + project.getName() + " does not exist: " + file );
                }
                Files.copy( path, licensesDir.resolve( project.getName() + "-" + path.getFileName() ) );
            }
            if ( licensing.getText() != null )
            {
                Files.writeString( licensesDir.resolve( project.getName() + "-" + "COPYING" ), licensing.getText(),
                                   StandardOpenOption.CREATE_NEW );
            }
        }
    }
}
