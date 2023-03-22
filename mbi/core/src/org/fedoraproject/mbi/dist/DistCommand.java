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
package org.fedoraproject.mbi.dist;

import java.net.URLClassLoader;
import java.nio.file.Path;

import org.fedoraproject.mbi.Command;
import org.fedoraproject.mbi.Reactor;
import org.fedoraproject.mbi.Util;
import org.fedoraproject.mbi.model.ModuleDescriptor;
import org.fedoraproject.mbi.tool.ToolUtils;

/**
 * @author Mikolaj Izdebski
 */
public class DistCommand
    extends Command
{
    private Reactor reactor;

    @Override
    public void run()
        throws Exception
    {
        reactor = Reactor.defaultReactor();
        ModuleDescriptor distModule = reactor.getModule( "mbi-dist" );

        Path workDir = reactor.getTargetDir( distModule ).resolve( "dist-work" );
        Util.delete( workDir );

        String javaCmdPath = getOption( "javaCmdPath", "/usr/lib/jvm/java-17/bin/java" );
        String basePackageName = getOption( "basePackageName", "mbi" );
        String installRoot = getOption( "installRoot", "/" );
        String mavenHomePath = getOption( "mavenHomePath", "/opt/mbi/maven" );
        String metadataPath = getOption( "metadataPath", "/opt/mbi/metadata" );
        String artifactsPath = getOption( "artifactsPath", "/opt/mbi/artifacts" );
        String launchersPath = getOption( "launchersPath", "/opt/mbi/bin" );
        String licensesPath = getOption( "licensesPath", "/opt/mbi/licenses" );
        DistRequest request = new DistRequest( reactor, workDir, javaCmdPath, basePackageName, //
                                               installRoot, mavenHomePath, metadataPath, artifactsPath, launchersPath,
                                               licensesPath );

        try ( URLClassLoader cl = ToolUtils.newClassLoader( reactor, distModule ) )
        {
            Thread.currentThread().setContextClassLoader( cl );
            Class<?> distMainClass = cl.loadClass( "org.fedoraproject.mbi.tool.dist.DistMain" );
            distMainClass.getMethod( "main", DistRequest.class ).invoke( null, request );
        }
    }
}
