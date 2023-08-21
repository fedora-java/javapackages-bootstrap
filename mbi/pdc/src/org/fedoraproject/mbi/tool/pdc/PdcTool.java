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
package org.fedoraproject.mbi.tool.pdc;

import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifact;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.apache.maven.tools.plugin.PluginToolsRequest;
import org.apache.maven.tools.plugin.extractor.MojoDescriptorExtractor;
import org.apache.maven.tools.plugin.generator.PluginDescriptorFilesGenerator;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.logging.Logger;
import org.fedoraproject.mbi.tool.Instruction;
import org.fedoraproject.mbi.tool.Tool;

/**
 * @author Mikolaj Izdebski
 */
public class PdcTool
    extends Tool
{
    private final PluginDescriptor pluginDescriptor = new PluginDescriptor();

    private final MavenProject mavenProject = new MavenProject();

    @Override
    public void initialize()
    {
        pluginDescriptor.setGroupId( "org.apache.maven.plugins" );
        pluginDescriptor.setArtifactId( getModule().getName() );
        pluginDescriptor.setVersion( getProject().getMBIVersion() );

        mavenProject.setArtifact( new ProjectArtifact( mavenProject ) );
        mavenProject.getBuild().setDirectory( getClassesDir().toString() );
        mavenProject.getBuild().setOutputDirectory( getClassesDir().toString() );
    }

    @Instruction
    public void groupId( String groupId )
    {
        pluginDescriptor.setGroupId( groupId );
    }

    @Instruction
    public void artifactId( String artifactId )
    {
        pluginDescriptor.setArtifactId( artifactId );
    }

    @Instruction
    public void gleanFromJavadoc( String path )
        throws Exception
    {
        mavenProject.addCompileSourceRoot( getSourceRootDir().resolve( path ).toString() );
    }

    @Override
    public void execute()
        throws Exception
    {
        PluginToolsRequest request = new DefaultPluginToolsRequest( mavenProject, pluginDescriptor );

        ContainerConfiguration conf = new DefaultContainerConfiguration();
        conf.setClassWorld( new ClassWorld( "plexus.core", Thread.currentThread().getContextClassLoader() ) );
        conf.setClassPathScanning( PlexusConstants.SCANNING_INDEX );
        conf.setAutoWiring( true );
        PlexusContainer container = new DefaultPlexusContainer( conf );
        try
        {
            container.lookup( Logger.class ).setThreshold( Logger.LEVEL_ERROR );
            for ( MojoDescriptorExtractor extractor : container.lookupList( MojoDescriptorExtractor.class ) )
            {
                for ( MojoDescriptor mojo : extractor.execute( request ) )
                {
                    pluginDescriptor.addMojo( mojo );
                }
            }
        }
        finally
        {
            container.dispose();
        }

        if ( pluginDescriptor.getMojos().isEmpty() )
        {
            throw new RuntimeException( "No MOJOs were discovered by PDC for module " + getModule().getName() );
        }

        PluginDescriptorFilesGenerator generator = new PluginDescriptorFilesGenerator();
        generator.execute( getClassesDir().resolve( "META-INF/maven" ).toFile(), request );
    }
}
