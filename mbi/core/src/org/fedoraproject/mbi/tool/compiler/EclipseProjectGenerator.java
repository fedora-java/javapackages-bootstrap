/*-
 * Copyright (c) 2020-2024 Red Hat, Inc.
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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.fedoraproject.mbi.Reactor;
import org.fedoraproject.mbi.model.ModuleDescriptor;
import org.fedoraproject.mbi.model.ProjectDescriptor;

/**
 * @author Mikolaj Izdebski
 */
class EclipseProjectGenerator
{
    private final Reactor reactor;

    private final ProjectDescriptor project;

    private final ModuleDescriptor module;

    private final int release;

    private final boolean accessInternalJavaAPI;

    private final Map<String, String> eclipseClasspath = new LinkedHashMap<>();

    private final Map<String, String> links = new LinkedHashMap<>();

    private final Path outputDir;

    public EclipseProjectGenerator( Reactor reactor, ProjectDescriptor project, ModuleDescriptor module, int release,
                                    boolean accessInternalJavaAPI )
    {
        this.reactor = reactor;
        this.project = project;
        this.module = module;
        this.release = release;
        this.accessInternalJavaAPI = accessInternalJavaAPI;
        outputDir = reactor.getRootDir().resolve( "eclipse" ).resolve( module.getName() );
    }

    public void generate()
        throws Exception
    {
        Files.createDirectories( outputDir.resolve( ".settings" ) );
        try ( BufferedWriter bw = Files.newBufferedWriter( outputDir.resolve( ".project" ) ) )
        {
            bw.write( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" );
            bw.write( "<projectDescription>\n" );
            bw.write( "  <name>" + module.getName() + "</name>\n" );
            bw.write( "  <buildSpec>\n" );
            bw.write( "    <buildCommand>\n" );
            bw.write( "      <name>org.eclipse.jdt.core.javabuilder</name>\n" );
            bw.write( "    </buildCommand>\n" );
            bw.write( "  </buildSpec>\n" );
            bw.write( "  <natures>\n" );
            bw.write( "    <nature>org.eclipse.jdt.core.javanature</nature>\n" );
            bw.write( "  </natures>\n" );
            bw.write( "  <linkedResources>\n" );
            for ( var link : links.entrySet() )
            {
                bw.write( "    <link>\n" );
                bw.write( "      <type>2</type>\n" );
                bw.write( "      <name>" + link.getKey() + "</name>\n" );
                bw.write( "      <locationURI>" + link.getValue() + "</locationURI>\n" );
                bw.write( "    </link>\n" );
            }
            bw.write( "  </linkedResources>\n" );
            bw.write( "</projectDescription>" );
        }
        String vm = release == 8 ? "1.8" : "" + release;
        try ( BufferedWriter bw = Files.newBufferedWriter( outputDir.resolve( ".classpath" ) ) )
        {
            bw.write( "<classpath>\n" );
            for ( var src : eclipseClasspath.entrySet() )
            {
                bw.write( "  <classpathentry\n");
                bw.write( "      kind=\"src\"\n" );
                bw.write( "      path=\"" + src.getKey() + "\"\n" );
                bw.write( "      excluding=\"" + src.getValue() + "\"\n" );
                bw.write( "  />\n" );
            }
            bw.write( "  <classpathentry\n");
            bw.write( "      kind=\"con\"\n" );
            bw.write( "      path=\"org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-21\"\n" );
            bw.write( "  />\n" );
            bw.write( "  <classpathentry\n");
            bw.write( "      kind=\"output\"\n" );
            bw.write( "      path=\"bin\"\n" );
            bw.write( "  />\n" );
            for ( String dep : module.getDependencies() )
            {
                bw.write( "  <classpathentry\n");
                bw.write( "      kind=\"src\"\n");
                bw.write( "      path=\"/" + dep + "\"\n" );
                bw.write( "      combineaccessrules=\"false\"\n" );
                bw.write( "  />\n" );
            }
            bw.write( "</classpath>\n" );
        }
        try ( BufferedWriter bw =
            Files.newBufferedWriter( outputDir.resolve( ".settings" ).resolve( "org.eclipse.jdt.core.prefs" ) ) )
        {
            bw.write( "eclipse.preferences.version=1\n" );
            bw.write( "org.eclipse.jdt.core.compiler.compliance=" + vm + "\n" );
            bw.write( "org.eclipse.jdt.core.compiler.source=" + vm + "\n" );
            bw.write( "org.eclipse.jdt.core.compiler.codegen.targetPlatform=" + vm + "\n" );
            if ( !accessInternalJavaAPI )
            {
                bw.write( "org.eclipse.jdt.core.compiler.release=enabled\n" );
            }
            bw.write( "org.eclipse.jdt.core.compiler.problem.forbiddenReference=warning\n" );
        }
        try ( BufferedWriter bw =
            Files.newBufferedWriter( outputDir.resolve( ".settings" ).resolve( "org.eclipse.core.resources.prefs" ) ) )
        {
            bw.write( "eclipse.preferences.version=1\n" );
            bw.write( "encoding/<project>=UTF-8\n" );
        }
    }

    public void addSourceDir( Path sourceDir, Collection<Path> excluded )
        throws Exception
    {
        Path baseDir = reactor.getProjectDir( project );
        Path outputDir = reactor.getRootDir().resolve( "eclipse" ).resolve( module.getName() );
        Path relSrcDir = outputDir.relativize( sourceDir );
        Path linkName;
        if ( sourceDir.startsWith( baseDir ) )
        {
            linkName = baseDir.relativize( sourceDir );
        }
        else if ( sourceDir.equals( reactor.getGeneratedSourcesDir( module ) ) )
        {
            linkName = sourceDir.getFileName();
        }
        else
        {
            throw new IllegalStateException();
        }
        int parentCount = 0;
        while ( relSrcDir.startsWith( Path.of( ".." ) ) )
        {
            parentCount++;
            relSrcDir = relSrcDir.subpath( 1, relSrcDir.getNameCount() );
        }
        if ( parentCount == 0 )
        {
            throw new IllegalStateException();
        }
        String uri = "$%7BPARENT-" + parentCount + "-PROJECT_LOC%7D/" + relSrcDir;
        links.put( linkName.toString(), uri );

        String excl =
            excluded.stream().map( sourceDir::relativize ).map( Path::toString ).collect( Collectors.joining( "|" ) );
        eclipseClasspath.put( linkName.toString(), excl );
    }
}
