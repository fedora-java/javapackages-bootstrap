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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
        Path mavenHome = dist.getMavenHomePath();
        Path homeTemplate =
            reactor.getSourceRootDir( reactor.getModule( "maven4-core" ) ).resolve( "../../apache-maven/src/assembly/maven" );
        for ( var subdir : Arrays.asList( "bin", "conf", "lib" ) )
        {
            Util.copy( homeTemplate.resolve( subdir ), dist.getInstallRoot().resolve( mavenHome ).resolve( subdir ),
                       p -> !p.getFileName().toString().startsWith( "." ) );
        }
        for ( String executable : Arrays.asList( "mvn", "mvnDebug", "mvnenc", "mvnsh", "mvnyjp" ) )
        {
            Files.setPosixFilePermissions( dist.getInstallRoot().resolve( mavenHome ).resolve( "bin" ).resolve( executable ),
                                           PosixFilePermissions.fromString( "rwxr-xr-x" ) );
        }
        for ( String moduleName : Arrays.asList( "aopalliance", "asm", "common-annotations-api", "commons-cli",
                                                 "commons-codec", "gson", "guava", "guice", "httpcomponents-client",
                                                 "httpcomponents-core", "injection-api", "jdom2", "jline3", "maven-resolver2",
                                                 "maven-wagon", "maven4-api-annotations", "maven4-api-cli",
                                                 "maven4-api-core", "maven4-api-di", "maven4-api-metadata",
                                                 "maven4-api-model", "maven4-api-plugin", "maven4-api-settings",
                                                 "maven4-api-spi", "maven4-api-toolchain", "maven4-api-xml",
                                                 "maven4-artifact", "maven4-builder-support", "maven4-cli",
                                                 "maven4-compat", "maven4-core", "maven4-di", "maven4-embedder",
                                                 "maven4-impl", "maven4-jline", "maven4-logging", "maven4-model",
                                                 "maven4-model-builder", "maven4-plugin-api",
                                                 "maven4-repository-metadata", "maven4-resolver-provider",
                                                 "maven4-settings", "maven4-settings-builder",
                                                 "maven4-toolchain-builder", "maven4-toolchain-model", "maven4-xml",
                                                 "plexus-component-annotations", "plexus-interactivity",
                                                 "plexus-interpolation", "plexus-sec-dispatcher4", "plexus-utils4",
                                                 "plexus-xml", "sisu-inject", "sisu-plexus", "slf4j2", "stax2-api",
                                                 "woodstox" ) )
        {
            symlink( mavenHome.resolve( "lib" ).resolve( moduleName + ".jar" ), distJarPath( moduleName ) );
        }
        symlink( mavenHome.resolve( "boot" ).resolve( "plexus-classworlds-X.jar" ),
                 distJarPath( "plexus-classworlds" ) );
        symlink( mavenHome.resolve( "lib/ext" ).resolve( "xmvn.jar" ), distJarPath( "xmvn" ) );
        symlink( mavenHome.resolve( "lib/ext" ).resolve( "kojan-xml.jar" ), distJarPath( "kojan-xml" ) );

        Path binDir = dist.getLaunchersPath();
        if ( !binDir.equals( mavenHome.resolve( "bin" ) ) )
        {
            symlink( binDir.resolve( "mvn" ), dist.getMavenHomePath().resolve( "bin/mvn" ) );
        }
        symlink( binDir.resolve( "xmvn" ), dist.getLaunchersPath().resolve( "mvn" ) );

        launcher( "xmvn-install", "org.fedoraproject.xmvn.tools.install.cli.InstallerCli", //
                  "xmvn", "slf4j", "commons-compress", "commons-io", "commons-codec", "commons-lang", "asm",
                  "kojan-xml", "picocli" );
        launcher( "xmvn-resolve", "org.fedoraproject.xmvn.tools.resolve.ResolverCli", //
                  "xmvn", "kojan-xml", "picocli" );
        launcher( "xmvn-subst", "org.fedoraproject.xmvn.tools.subst.SubstCli", //
                  "xmvn", "kojan-xml", "picocli" );
        launcher( "cup", "java_cup.Main", "cup" );
        launcher( "jflex", "jflex.Main", "jflex", "cup" );
        launcher( "ant", "org.apache.tools.ant.launch.Launcher", "ant" );

        System.err.println( "BUILD SUCCESS" );
    }

    private void symlink( Path link, Path target )
        throws IOException
    {
        Files.createDirectories( dist.getInstallRoot().resolve( link.getParent() ) );
        Files.createSymbolicLink( dist.getInstallRoot().resolve( link ), link.getParent().relativize( target ) );
    }

    private Path distJarPath( String moduleName )
    {
        reactor.getModule( moduleName ); // Ensure module with given name exists
        return dist.getArtifactsPath().resolve( dist.getBasePackageName() ).resolve( mod2art.get( moduleName )
            + ".jar" );
    }

    private void launcher( String launcherName, String mainClass, String... classPath )
        throws IOException
    {
        Path launcherPath = dist.getInstallRoot().resolve( dist.getLaunchersPath() ).resolve( launcherName );
        Manifest mf = new Manifest();
        Attributes attr = mf.getMainAttributes();
        attr.put( MANIFEST_VERSION, "1.0" );
        attr.put( MAIN_CLASS, mainClass );
        attr.put( CLASS_PATH, String.join( " ", Arrays.asList( classPath ).stream() //
                                                      .map( this::distJarPath ) //
                                                      .map( dist.getLaunchersPath()::relativize ) //
                                                      .map( Path::toString ) //
                                                      .toArray( String[]::new ) ) );
        try ( OutputStream os = Files.newOutputStream( launcherPath ) )
        {
            Files.setPosixFilePermissions( launcherPath, PosixFilePermissions.fromString( "rwxr-xr-x" ) );
            os.write( ( "#!" + dist.getJavaCmdPath() + " -jar\n" ).getBytes( StandardCharsets.UTF_8 ) );
            try ( Closeable jos = new JarOutputStream( os, mf ) )
            {
                // Empty JAR
            }
        }
    }
}
