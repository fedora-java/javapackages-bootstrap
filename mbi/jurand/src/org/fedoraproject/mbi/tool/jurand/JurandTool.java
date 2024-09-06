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
package org.fedoraproject.mbi.tool.jurand;

import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.file.PathUtils;
import org.fedoraproject.mbi.tool.Instruction;
import org.fedoraproject.mbi.tool.Tool;

/**
 * @author Mikolaj Izdebski
 */
public class JurandTool
    extends Tool
{
    private final List<String> cmd = new ArrayList<>();

    @Override
    public void initialize()
        throws Exception
    {
        Files.createDirectories( getGeneratedSourcesDir() );
        cmd.add( "jurand" );
        cmd.add( "-i" );
        cmd.add( "-s" );
        cmd.add( "-a" );
    }

    @Instruction
    public void source( String source )
        throws Exception
    {
        PathUtils.copyDirectory( getSourceRootDir().resolve( source ), getGeneratedSourcesDir() );
    }

    @Instruction
    public void name( String name )
    {
        cmd.add( "-n" );
        cmd.add( name );
    }

    @Instruction
    public void pattern( String pattern )
    {
        cmd.add( "-p" );
        cmd.add( pattern );
    }

    @Override
    public void execute()
        throws Exception
    {
        cmd.add( getGeneratedSourcesDir().toString() );
        ProcessBuilder pb = new ProcessBuilder( cmd );
        pb.redirectInput( Redirect.PIPE );
        pb.redirectOutput( getTargetDir().resolve( "jurand-stdout.log" ).toFile() );
        pb.redirectError( getTargetDir().resolve( "jurand-stderr.log" ).toFile() );
        Process process = pb.start();
        int exitCode = process.waitFor();
        if ( exitCode != 0 )
        {
            throw new RuntimeException( "jurand failed with exit code " + exitCode );
        }
    }
}
