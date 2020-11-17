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

import static java.util.jar.Attributes.Name.CLASS_PATH;
import static java.util.jar.Attributes.Name.MAIN_CLASS;
import static java.util.jar.Attributes.Name.MANIFEST_VERSION;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.fedoraproject.mbi.Reactor;
import org.fedoraproject.mbi.Util;
import org.fedoraproject.mbi.dist.DistRequest;

/**
 * @author Mikolaj Izdebski
 */
public class MavenDist
{
    private final Reactor reactor;

    private final DistRequest dist;

    private final Map<String, String> mod2art;

    public MavenDist( DistRequest dist, Map<String, String> mod2art )
    {
        this.reactor = dist.getReactor();
        this.dist = dist;
        this.mod2art = mod2art;
    }

    public void doDist()
        throws Exception
    {
        Path mavenHome = dist.getInstallRoot().resolve( dist.getMavenHomePath() );
        Path homeTemplate =
            reactor.getSourceRootDir( reactor.getModule( "maven-core" ) ).resolve( "../apache-maven/src" );
        for ( var subdir : Arrays.asList( "bin", "conf", "lib" ) )
        {
            Util.copy( homeTemplate.resolve( subdir ), mavenHome.resolve( subdir ), x -> true );
        }
        for ( String moduleName : Arrays.asList( "asm", "common-annotations-api", "commons-cli", "commons-io",
                                                 "commons-lang", "guava", "guice", "injection-api", "jansi", "jsr-305",
                                                 "maven-artifact", "maven-builder-support", "maven-compat",
                                                 "maven-core", "maven-embedder", "maven-model-builder", "maven-model",
                                                 "maven-plugin-api", "maven-repository-metadata",
                                                 "maven-resolver-provider", "maven-settings-builder", "maven-settings",
                                                 "maven-resolver", "maven-shared-utils", "maven-wagon", "plexus-cipher",
                                                 "plexus-classworlds", "plexus-component-annotations",
                                                 "plexus-interpolation", "plexus-sec-dispatcher", "plexus-utils",
                                                 "sisu-inject", "sisu-plexus", "slf4j" ) )
        {
            linkJar( moduleName, mavenHome.resolve( "lib" ).resolve( moduleName + ".jar" ) );
        }
        linkJar( "plexus-classworlds", mavenHome.resolve( "boot" ).resolve( "plexus-classworlds-X.jar" ) );
        linkJar( "xmvn", mavenHome.resolve( "lib/ext" ).resolve( "xmvn.jar" ) );

        Path binDir = dist.getInstallRoot().resolve( dist.getLaunchersPath() );
        if ( !binDir.equals( mavenHome.resolve( "bin" ) ) )
        {
            Files.createDirectories( binDir );
            Files.createSymbolicLink( binDir.resolve( "mvn" ),
                                      Paths.get( "/" ).resolve( dist.getMavenHomePath() ).resolve( "bin/mvn" ) );
        }
        Files.createSymbolicLink( binDir.resolve( "xmvn" ), Paths.get( "mvn" ) );

        launcher( binDir.resolve( "xmvn-install" ), "org.fedoraproject.xmvn.tools.install.cli.InstallerCli", //
                  "xmvn", "jcommander", "slf4j", "commons-compress", "asm" );
        launcher( binDir.resolve( "xmvn-resolve" ), "org.fedoraproject.xmvn.tools.resolve.ResolverCli", //
                  "xmvn", "jcommander" );
        launcher( binDir.resolve( "xmvn-subst" ), "org.fedoraproject.xmvn.tools.subst.SubstCli", //
                  "xmvn", "jcommander" );

        launcher( binDir.resolve( "cup" ), "java_cup.Main", "cup" );
        launcher( binDir.resolve( "jflex" ), "jflex.Main", "jflex", "cup" );
        launcher( binDir.resolve( "ant" ), "org.apache.tools.ant.launch.Launcher", "ant" );

        System.err.println( "BUILD SUCCESS" );
    }

    private Path distJarPath( String moduleName )
    {
        return dist.getArtifactsPath().resolve( dist.getBasePackageName() ).resolve( mod2art.get( moduleName )
            + ".jar" );
    }

    private void linkJar( String moduleName, Path dest )
        throws IOException
    {
        reactor.getModule( moduleName ); // Ensure module with given name exists
        Files.createDirectories( dest.getParent() );
        Files.createSymbolicLink( dest, Paths.get( "/" ).resolve( distJarPath( moduleName ) ) );
    }

    private void launcher( Path launcherPath, String mainClass, String... classPath )
        throws IOException
    {
        Manifest mf = new Manifest();
        Attributes attr = mf.getMainAttributes();
        attr.put( MANIFEST_VERSION, "1.0" );
        attr.put( MAIN_CLASS, mainClass );
        attr.put( CLASS_PATH,
                  String.join( " ",
                               Arrays.asList( classPath ).stream().map( p -> distJarPath( p ) ).map( Paths.get( "/" )::resolve ).map( Path::toString ).toArray( String[]::new ) ) );
        try ( OutputStream os = Files.newOutputStream( launcherPath ) )
        {
            Files.setPosixFilePermissions( launcherPath, PosixFilePermissions.fromString( "rwxrwxr-x" ) );
            os.write( ( "#!" + dist.getJavaCmdPath() + " -jar\n" ).getBytes() );
            try ( Closeable jos = new JarOutputStream( os, mf ) )
            {
                // Empty JAR
            }
        }
    }
}
