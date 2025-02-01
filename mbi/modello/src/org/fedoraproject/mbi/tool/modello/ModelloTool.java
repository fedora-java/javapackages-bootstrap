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
package org.fedoraproject.mbi.tool.modello;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codehaus.modello.core.ModelloCore;
import org.codehaus.modello.model.Model;
import org.codehaus.modello.plugin.velocity.VelocityGenerator;
import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.fedoraproject.mbi.tool.Instruction;
import org.fedoraproject.mbi.tool.Tool;

/**
 * @author Mikolaj Izdebski
 */
public class ModelloTool
    extends Tool
{
    private final Map<String, Object> params = new LinkedHashMap<>();

    private String model;

    private String output;

    private List<String> templates = new ArrayList<>();

    private Map<String, String> velocityParams = new LinkedHashMap<>();

    public ModelloTool()
        throws Exception
    {
        params.put( "modello.dom.xpp3", "true" );
        params.put( "modello.package.with.version", "false" );
        params.put( "modello.output.java.source", "8" );
        params.put( "modello.output.encoding", "UTF-8" );
    }

    @Instruction
    public void model( String model )
    {
        this.model = model;
    }

    @Instruction
    public void version( String version )
    {
        params.put( "modello.version", version );
    }

    @Instruction
    public void output( String output )
    {
        this.output = output;
    }

    @Instruction
    public void xpp3dom( String xpp3dom )
    {
        params.put( "modello.dom.xpp3", xpp3dom );
    }

    @Instruction
    public void velocityBasedir( String dir )
    {
        params.put( VelocityGenerator.VELOCITY_BASEDIR, getSourceRootDir().resolve( dir ).toString() );
    }

    @Instruction
    public void template( String template )
    {
        templates.add( template );
    }

    @Instruction
    public void param( String s )
    {
        velocityParams.put( s.substring( 0, s.indexOf( '=' ) ), s.substring( s.indexOf( '=' ) + 1 ) );
    }

    @Override
    public void execute()
        throws Exception
    {
        Files.createDirectories( getGeneratedSourcesDir() );
        Path modelPath = getSourceRootDir().resolve( model );
        params.put( "modello.output.directory", getGeneratedSourcesDir().toString() );
        params.put( VelocityGenerator.VELOCITY_TEMPLATES, templates.stream().collect( Collectors.joining( "," ) ) );
        params.put( VelocityGenerator.VELOCITY_PARAMETERS, velocityParams );

        ContainerConfiguration conf = new DefaultContainerConfiguration();
        conf.setClassPathScanning( PlexusConstants.SCANNING_INDEX );
        conf.setAutoWiring( true );
        PlexusContainer container = new DefaultPlexusContainer( conf );
        ModelloCore modello = container.lookup( ModelloCore.class );
        try ( Reader modelReader = Files.newBufferedReader( modelPath ) )
        {
            Model model = modello.loadModel( modelReader );
            for ( String generator : output.split( "\\|" ) )
            {
                modello.generate( model, generator, params );
            }
        }
    }
}
