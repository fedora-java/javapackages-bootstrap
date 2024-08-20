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
package org.fedoraproject.mbi.tool.compiler;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
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

    public EclipseProjectGenerator( Reactor reactor, ProjectDescriptor project, ModuleDescriptor module, int release,
                                    boolean accessInternalJavaAPI )
    {
        this.reactor = reactor;
        this.project = project;
        this.module = module;
        this.release = release;
        this.accessInternalJavaAPI = accessInternalJavaAPI;
    }

    private final StringBuilder eclipseClasspath = new StringBuilder( "<classpath>" );

    private final StringBuilder links = new StringBuilder();

    public void generate()
        throws Exception
    {
        Path outputDir = reactor.getRootDir().resolve( "eclipse" ).resolve( module.getName() );
        Files.createDirectories( outputDir.resolve( ".settings" ) );
        try ( BufferedWriter bw = Files.newBufferedWriter( outputDir.resolve( ".project" ) ) )
        {
            bw.write( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + //
                "<projectDescription>\n" + //
                "\t<name>" + module.getName() + "</name>\n" + //
                "\t<comment></comment>\n" + //
                "\t<projects>\n" + //
                "\t</projects>\n" + //
                "\t<buildSpec>\n" + //
                "\t\t<buildCommand>\n" + //
                "\t\t\t<name>org.eclipse.jdt.core.javabuilder</name>\n" + //
                "\t\t\t<arguments>\n" + //
                "\t\t\t</arguments>\n" + //
                "\t\t</buildCommand>\n" + //
                "\t</buildSpec>\n" + //
                "\t<natures>\n" + //
                "\t\t<nature>org.eclipse.jdt.core.javanature</nature>\n" + //
                "\t</natures>\n" + //
                "\t<linkedResources>\n" + //
                links.toString() + //
                "\t</linkedResources>\n" + //
                "</projectDescription>" );
        }
        String vm = release == 8 ? "1.8" : "" + release;
        eclipseClasspath.append( "<classpathentry kind=\"con\""
            + " path=\"org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-17\"/>\n" );
        eclipseClasspath.append( "<classpathentry kind=\"output\" path=\"bin\"/>\n" );
        try ( BufferedWriter bw = Files.newBufferedWriter( outputDir.resolve( ".classpath" ) ) )
        {
            StringBuilder sb = new StringBuilder( eclipseClasspath.toString() );
            for ( String dep : module.getDependencies() )
            {
                sb.append( "<classpathentry combineaccessrules=\"false\" kind=\"src\" path=\"/" + dep + "\"/>\n" );
            }
            bw.write( sb.toString() + "</classpath>" );
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
        links.append( "" + //
            "\t\t<link>\n" + //
            "\t\t\t<name>" + linkName + "</name>\n" + //
            "\t\t\t<type>2</type>\n" + //
            "\t\t\t<locationURI>" + uri + "</locationURI>\n" + //
            "\t\t</link>\n" );

        String excl =
            excluded.stream().map( sourceDir::relativize ).map( Path::toString ).collect( Collectors.joining( "|" ) );
        eclipseClasspath.append( "<classpathentry kind=\"src\" path=\"" + linkName + "\" excluding=\"" + excl
            + "\"/>\n" );
    }
}
