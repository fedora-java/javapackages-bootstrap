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
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.fedoraproject.mbi.Util;
import org.fedoraproject.mbi.tool.Instruction;
import org.fedoraproject.mbi.tool.Tool;

/**
 * @author Mikolaj Izdebski
 */
public class CompilerTool
    extends Tool
{
    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    private final StandardJavaFileManager fileManager = compiler.getStandardFileManager( null, null, null );

    private final List<String> sourceRoots = new ArrayList<>();

    private final List<String> resources = new ArrayList<>();

    private Predicate<Path> sourceFilter = source -> true;

    private String automaticModuleName;

    private int release = 8;

    private boolean accessInternalJavaAPI;

    private final List<String> options = new ArrayList<>();

    @Instruction
    public void release( String release )
    {
        this.release = Integer.parseInt( release );
    }

    @Instruction
    public void accessInternalJavaAPI( String dummy )
    {
        this.accessInternalJavaAPI = true;
    }

    @Instruction
    public void addSourceRoot( String sourceRoot )
    {
        sourceRoots.add( sourceRoot );
    }

    @Instruction
    public void excludeSourceClass( String className )
    {
        sourceFilter = sourceFilter.and( path -> !path.toString().endsWith( "/" + className + ".java" ) );
    }

    @Instruction
    public void excludeSourceMatching( String regex )
    {
        sourceFilter = sourceFilter.and( path -> !path.toString().matches( ".*" + regex ) );
    }

    @Instruction
    public void addResource( String resource )
    {
        resources.add( resource );
    }

    @Instruction
    public void automaticModuleName( String name )
    {
        automaticModuleName = name;
    }

    @Instruction
    public void option( String option )
    {
        options.add( option );
    }

    @Override
    public void execute()
        throws Exception
    {
        for ( String resource : resources )
        {
            Util.copy( getSourceRootDir().resolve( resource ), getClassesDir(),
                       path -> !path.getFileName().toString().endsWith( ".java" ) );
        }
        List<Path> sourceDirs = new ArrayList<>();
        if ( Files.exists( getGeneratedSourcesDir() ) )
        {
            sourceDirs.add( getGeneratedSourcesDir() );
        }
        for ( String sourceRoot : sourceRoots )
        {
            sourceDirs.add( getSourceRootDir().resolve( sourceRoot ) );
        }
        List<Path> allIncluded = new ArrayList<>();
        MavenProjectGenerator maven =
            new MavenProjectGenerator( getReactor(), getModule(), release, accessInternalJavaAPI );
        EclipseProjectGenerator eclipse =
            new EclipseProjectGenerator( getReactor(), getProject(), getModule(), release, accessInternalJavaAPI );
        for ( Path sourceDir : sourceDirs )
        {
            List<Path> included = new ArrayList<>();
            List<Path> excluded = new ArrayList<>();
            Util.filterJavaSources( included, excluded, sourceDir, sourceFilter );
            allIncluded.addAll( included );
            maven.addSourceDir( sourceDir, excluded );
            eclipse.addSourceDir( sourceDir, excluded );
        }
        maven.generate();
        eclipse.generate();
        Iterable<? extends JavaFileObject> compilationUnits =
            fileManager.getJavaFileObjectsFromFiles( allIncluded.stream().map( Path::toFile ).collect( Collectors.toList() ) );
        options.add( "-g" );
        options.add( "-d" );
        options.add( getClassesDir().toString() );
        // If internal Java APIs need to be visible then --release can't be used
        // https://bugs.openjdk.org/browse/JDK-8206937
        if ( accessInternalJavaAPI )
        {
            options.add( "-source" );
            options.add( release + "" );
            options.add( "-target" );
            options.add( release + "" );
        }
        else
        {
            options.add( "--release" );
            options.add( release + "" );
        }
        options.add( "-cp" );
        options.add( getClassPath().stream().map( Path::toString ).collect( Collectors.joining( ":" ) ) );
        StringWriter compilerOutput = new StringWriter();
        CompilationTask task = compiler.getTask( compilerOutput, fileManager, null, options, null, compilationUnits );
        boolean success = task.call();
        if ( !success )
        {
            System.err.print( compilerOutput.toString() );
            throw new Exception( "Compilation failed" );
        }
        if ( automaticModuleName != null )
        {
            Files.createDirectories( getClassesDir().resolve( "META-INF" ) );
            try ( BufferedWriter bw =
                Files.newBufferedWriter( getClassesDir().resolve( "META-INF" ).resolve( "MANIFEST.MF" ),
                                         StandardOpenOption.APPEND, StandardOpenOption.CREATE ) )
            {
                bw.write( "Automatic-Module-Name: " + automaticModuleName + "\n" );
            }
        }
    }
}
