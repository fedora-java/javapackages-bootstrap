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
package org.fedoraproject.mbi.tool.ant;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.tools.ant.ExitException;
import org.apache.tools.ant.Main;
import org.fedoraproject.mbi.tool.Instruction;
import org.fedoraproject.mbi.tool.ThreadUnsafe;
import org.fedoraproject.mbi.tool.Tool;

/**
 * @author Mikolaj Izdebski
 */
@ThreadUnsafe
public class AntTool
    extends Tool
{
    private String execution;

    private final Map<String, String> taskdefs = new LinkedHashMap<>();

    @Instruction
    public void run( String arg )
        throws IOException
    {
        this.execution = arg.replaceAll( "\\[\\[", "\u0001" ) //
                            .replaceAll( "\\]\\]", "\u0002" ) //
                            .replace( '[', '<' ) //
                            .replace( ']', '>' ) //
                            .replace( '\u0001', '[' ) //
                            .replace( '\u0002', ']' );
    }

    @Instruction
    public void taskdef( String arg )
        throws IOException
    {
        String[] args = arg.split( "=" );
        taskdefs.put( args[0], args[1] );
    }

    @Override
    public void execute()
        throws Exception
    {
        Path baseDir = getReactor().getSourceRootDir( getModule() );
        Path classesDir = getReactor().getClassesDir( getModule() );
        Path generatedSourcesDir = getReactor().getGeneratedSourcesDir( getModule() );
        Path buildFile = getReactor().getTargetDir( getModule() ).resolve( "ant-run.xml" );
        Files.createDirectories( buildFile.getParent() );

        try ( OutputStream os = Files.newOutputStream( buildFile ); PrintStream ps = new PrintStream( os ) )
        {
            ps.println( "<project default=\"antrun\" basedir=\"" + baseDir + "\">" );
            ps.println( "<property name=\"classes\" location=\"" + classesDir + "\"/>" );
            ps.println( "<property name=\"generatedSources\" location=\"" + generatedSourcesDir + "\"/>" );
            for ( var entry : taskdefs.entrySet() )
            {
                ps.println( "<taskdef name=\"" + entry.getKey() + "\" classname=\"" + entry.getValue() + "\"/>" );
            }
            ps.println( "<target name=\"antrun\">" );
            ps.println( execution );
            ps.println( "</target>" );
            ps.println( "</project>" );
        }

        Exception exception = null;
        try
        {
            new Main()
            {
                protected void exit( int exitCode )
                {
                    throw new ExitException( exitCode );
                }
            }.startAnt( new String[] { "-f", buildFile.toString() }, null, null );
        }
        catch ( ExitException e )
        {
            if ( e.getStatus() != 0 )
            {
                exception = e;
            }
        }

        if ( exception != null )
        {
            throw exception;
        }
    }
}
