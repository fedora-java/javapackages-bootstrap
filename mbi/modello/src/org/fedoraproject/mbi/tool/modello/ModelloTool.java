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
import java.util.Properties;

import org.codehaus.modello.Modello;
import org.fedoraproject.mbi.tool.Instruction;
import org.fedoraproject.mbi.tool.Tool;

/**
 * @author Mikolaj Izdebski
 */
public class ModelloTool
    extends Tool
{
    private final Properties params = new Properties();

    private String model;

    private String output;

    public ModelloTool()
        throws Exception
    {
        params.setProperty( "modello.dom.xpp3", "true" );
        params.setProperty( "modello.package.with.version", "false" );
        params.setProperty( "modello.output.useJava5", "true" );
        params.setProperty( "modello.output.encoding", "UTF-8" );
    }

    @Instruction
    public void model( String model )
    {
        this.model = model;
    }

    @Instruction
    public void version( String version )
    {
        params.setProperty( "modello.version", version );
    }

    @Instruction
    public void output( String output )
    {
        this.output = output;
    }

    @Instruction
    public void xpp3dom( String xpp3dom )
    {
        params.setProperty( "modello.dom.xpp3", xpp3dom );
    }

    @Override
    public void execute()
        throws Exception
    {
        Files.createDirectories( getGeneratedSourcesDir() );
        Path modelPath = getSourceRootDir().resolve( model );
        params.setProperty( "modello.output.directory", getGeneratedSourcesDir().toString() );

        Modello modello = new Modello();
        for ( String generator : output.split( "\\|" ) )
        {
            try ( Reader modelReader = Files.newBufferedReader( modelPath ) )
            {
                modello.generate( modelReader, generator, params );
            }
        }
    }
}
