/*-
 * Copyright (c) 2024 Red Hat, Inc.
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
package org.fedoraproject.mbi.tool.compiler;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.fedoraproject.mbi.Reactor;
import org.fedoraproject.mbi.model.ModuleDescriptor;

/**
 * @author Mikolaj Izdebski
 */
class MavenProjectGenerator
{
    private final Reactor reactor;

    private final ModuleDescriptor module;

    private final int release;

    private final boolean accessInternalJavaAPI;

    private final List<String> sources = new ArrayList<>();

    private final List<String> excludes = new ArrayList<>();

    private static final Set<String> mbiModules = new TreeSet<>();

    private static final Set<String> otherModules = new TreeSet<>();

    private final Path outputDir;

    public MavenProjectGenerator( Reactor reactor, ModuleDescriptor module, int release, boolean accessInternalJavaAPI )
    {
        this.reactor = reactor;
        this.module = module;
        this.release = release;
        this.accessInternalJavaAPI = accessInternalJavaAPI;
        outputDir = reactor.getRootDir().resolve( "maven" ).resolve( module.getName() );
    }

    public void generate()
        throws Exception
    {
        String version = reactor.getProject( module.getProjectName() ).getMBIVersion();
        Files.createDirectories( outputDir );
        try ( BufferedWriter bw = Files.newBufferedWriter( outputDir.resolve( "pom.xml" ) ) )
        {
            bw.write( "<project>\n" );
            bw.write( "  <modelVersion>4.0.0</modelVersion>\n" );
            bw.write( "  <parent>\n" );
            bw.write( "    <groupId>mbi</groupId>\n" );
            bw.write( "    <artifactId>mbi-aggregator</artifactId>\n" );
            bw.write( "    <version>MBI</version>\n" );
            bw.write( "    <relativePath>../pom.xml</relativePath>\n" );
            bw.write( "  </parent>\n" );
            bw.write( "  <artifactId>" + module.getName() + "</artifactId>\n" );
            bw.write( "  <version>" + version + "</version>\n" );
            if ( !module.getDependencies().isEmpty() )
            {
                bw.write( "  <dependencies>\n" );
                for ( String dep : module.getDependencies() )
                {
                    String depVersion = reactor.getProject( reactor.getModule( dep ).getProjectName() ).getMBIVersion();
                    bw.write( "    <dependency>\n" );
                    bw.write( "      <groupId>mbi</groupId>\n" );
                    bw.write( "      <artifactId>" + dep + "</artifactId>\n" );
                    bw.write( "      <version>" + depVersion + "</version>\n" );
                    bw.write( "    </dependency>\n" );
                }
                bw.write( "  </dependencies>\n" );
            }
            bw.write( "  <build>\n" );
            bw.write( "    <plugins>\n" );
            bw.write( "      <plugin>\n" );
            bw.write( "        <groupId>org.apache.maven.plugins</groupId>\n" );
            bw.write( "        <artifactId>maven-compiler-plugin</artifactId>\n" );
            bw.write( "        <configuration>\n" );
            if ( accessInternalJavaAPI )
            {
                bw.write( "          <source>" + release + "</source>\n" );
                bw.write( "          <target>" + release + "</target>\n" );
            }
            else
            {
                bw.write( "          <release>" + release + "</release>\n" );
            }
            if ( !excludes.isEmpty() )
            {
                bw.write( "          <excludes>\n" );
                for ( String excl : excludes )
                {
                    bw.write( "            <exclude>" + excl + "</exclude>\n" );
                }
                bw.write( "          </excludes>\n" );
            }
            bw.write( "        </configuration>\n" );
            bw.write( "      </plugin>\n" );
            bw.write( "      <plugin>\n" );
            bw.write( "        <groupId>org.codehaus.mojo</groupId>\n" );
            bw.write( "        <artifactId>build-helper-maven-plugin</artifactId>\n" );
            bw.write( "        <configuration>\n" );
            if ( !sources.isEmpty() )
            {
                bw.write( "          <sources>\n" );
                for ( String source : sources )
                {
                    bw.write( "            <source>" + source + "</source>\n" );
                }
                bw.write( "          </sources>\n" );
            }
            bw.write( "        </configuration>\n" );
            bw.write( "      </plugin>\n" );
            bw.write( "    </plugins>\n" );
            bw.write( "  </build>\n" );
            bw.write( "</project>\n" );
        }
        synchronized ( mbiModules )
        {
            if ( module.getName().startsWith( "mbi-" ) )
            {
                mbiModules.add( module.getName() );
            }
            else
            {
                otherModules.add( module.getName() );
            }
            try ( BufferedWriter bw = Files.newBufferedWriter( outputDir.resolve( "../pom.xml" ) ) )
            {
                bw.write( "<project>\n" );
                bw.write( "  <modelVersion>4.0.0</modelVersion>\n" );
                bw.write( "  <parent>\n" );
                bw.write( "    <groupId>mbi</groupId>\n" );
                bw.write( "    <artifactId>mbi-parent</artifactId>\n" );
                bw.write( "    <version>MBI</version>\n" );
                bw.write( "    <relativePath>../pom.xml</relativePath>\n" );
                bw.write( "  </parent>\n" );
                bw.write( "  <artifactId>mbi-aggregator</artifactId>\n" );
                bw.write( "  <packaging>pom</packaging>\n" );
                bw.write( "  <modules>\n" );
                for ( String module : mbiModules )
                {
                    bw.write( "    <module>" + module + "</module>\n" );
                }
                bw.write( "  </modules>\n" );
                bw.write( "  <profiles>\n" );
                bw.write( "    <profile>\n" );
                bw.write( "      <id>third-party</id>\n" );
                bw.write( "      <activation>\n" );
                bw.write( "        <activeByDefault>true</activeByDefault>\n" );
                bw.write( "      </activation>\n" );
                bw.write( "      <modules>\n" );
                for ( String module : otherModules )
                {
                    bw.write( "        <module>" + module + "</module>\n" );
                }
                bw.write( "      </modules>\n" );
                bw.write( "    </profile>\n" );
                bw.write( "  </profiles>\n" );
                bw.write( "</project>\n" );
            }
        }
    }

    public void addSourceDir( Path sourceDir, Collection<Path> excluded )
        throws Exception
    {
        sources.add( outputDir.relativize( sourceDir ).toString() );
        excludes.addAll( excluded.stream().map( sourceDir::relativize ).map( Path::toString ).collect( Collectors.toList() ) );
    }
}
