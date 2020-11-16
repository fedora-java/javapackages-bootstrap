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
package org.fedoraproject.mbi.tool.pom;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.maven.model.Build;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.fedoraproject.mbi.tool.Instruction;
import org.fedoraproject.mbi.tool.Tool;

/**
 * @author Mikolaj Izdebski
 */
class ModelProcessor<X>
{
    private final Class<X> type;

    private final Function<X, String> mapping;

    private final String key;

    private boolean matched;

    public ModelProcessor( Class<X> type, Function<X, String> mapping, String key )
    {
        this.type = type;
        this.mapping = mapping;
        this.key = key;
    }

    private <T> void processSingle( T object, Consumer<T> processor )
    {
        if ( object != null )
        {
            processor.accept( object );
        }
    }

    private <T> void processEach( List<T> list, Consumer<T> processor )
    {
        for ( Iterator<T> iterator = list.iterator(); iterator.hasNext(); )
        {
            T object = iterator.next();
            if ( type.isInstance( object ) && mapping.apply( type.cast( object ) ).equals( key ) )
            {
                iterator.remove();
                matched = true;
            }
            else if ( processor != null )
            {
                processor.accept( object );
            }
        }
    }

    public boolean processModel( Model model )
    {
        processSingle( model.getBuild(), this::processBuild );
        processEach( model.getProfiles(), this::processProfile );
        return matched;
    }

    private void processProfile( Profile profile )
    {
        processSingle( profile.getBuild(), this::processBuildBase );
    }

    private void processBuildBase( BuildBase build )
    {
        processSingle( build.getPluginManagement(), this::processPluginManagement );
        processEach( build.getPlugins(), this::processPlugin );
    }

    private void processBuild( Build build )
    {
        processBuildBase( build );
        processEach( build.getExtensions(), null );
    }

    private void processPluginManagement( PluginManagement pluginManagement )
    {
        processEach( pluginManagement.getPlugins(), this::processPlugin );
    }

    private void processPlugin( Plugin plugin )
    {
        processEach( plugin.getExecutions(), null );
    }
}

/**
 * @author Mikolaj Izdebski
 */
public class PomTool
    extends Tool
{
    private Model model;

    private <T> void process( Class<T> type, Function<T, String> mapping, String key )
    {
        if ( !new ModelProcessor<>( type, mapping, key ).processModel( model ) )
        {
            throw new RuntimeException( "Unable to match " + type.getSimpleName() + "(" + key + ")" );
        }
    }

    private static Model readModel( Path path )
        throws Exception
    {
        try ( Reader reader = Files.newBufferedReader( path, StandardCharsets.UTF_8 ) )
        {
            return new MavenXpp3Reader().read( reader );
        }
    }

    private static void writeModel( Path path, Model model )
        throws Exception
    {
        try ( Writer writer = Files.newBufferedWriter( path, StandardCharsets.UTF_8 ) )
        {
            new MavenXpp3Writer().write( writer, model );
        }
    }

    @Override
    public void initialize()
        throws Exception
    {
        Path masterPom = getReactor().getSourceRootDir( getModule() ).resolve( "pom.xml" );
        if ( Files.isRegularFile( masterPom ) )
        {
            model = readModel( masterPom );
        }
    }

    @Instruction
    public void removeProfile( String id )
        throws Exception
    {
        process( Profile.class, Profile::getId, id );
    }

    @Instruction
    public void removeBuildExtensions( String dummy )
        throws Exception
    {
        process( Extension.class, extension -> "", "" );
    }

    @Instruction
    public void removePlugin( String artifactId )
        throws Exception
    {
        process( Plugin.class, Plugin::getArtifactId, artifactId );
    }

    @Instruction
    public void removePluginExecution( String id )
        throws Exception
    {
        process( PluginExecution.class, PluginExecution::getId, id );
    }

    @Override
    public void execute()
        throws Exception
    {
        Path pomPath = getReactor().getPomPath( getModule() );
        Files.createDirectories( pomPath.getParent() );
        writeModel( pomPath, model );
        Path pomPropsPath = getReactor().getPomPropertiesPath( getModule() );
        try ( Writer writer = Files.newBufferedWriter( pomPropsPath, StandardCharsets.UTF_8 ) )
        {
            Properties props = new Properties();
            props.setProperty( "groupId",
                               model.getGroupId() != null ? model.getGroupId() : model.getParent().getGroupId() );
            props.setProperty( "artifactId", model.getArtifactId() );
            props.setProperty( "version",
                               model.getVersion() != null ? model.getVersion() : model.getParent().getVersion() );
            props.store( writer, "Generated by MBI" );
        }
    }
}
