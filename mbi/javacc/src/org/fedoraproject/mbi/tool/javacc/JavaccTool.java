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
package org.fedoraproject.mbi.tool.javacc;

import java.util.ArrayList;
import java.util.List;

import org.fedoraproject.mbi.tool.Instruction;
import org.fedoraproject.mbi.tool.ThreadUnsafe;
import org.fedoraproject.mbi.tool.Tool;
import org.javacc.jjtree.JJTree;

/**
 * @author Mikolaj Izdebski
 */
@ThreadUnsafe
public class JavaccTool
    extends Tool
{
    private boolean runJjtree;

    private List<String> args = new ArrayList<>();

    @Instruction
    public void jjtree( String dummy )
    {
        runJjtree = true;
    }

    @Instruction
    public void arg( String arg )
    {
        arg = arg.replaceAll( "\\$\\{basedir}", getSourceRootDir().toString() );
        arg = arg.replaceAll( "\\$\\{generatedSources}", getGeneratedSourcesDir().toString() );
        args.add( arg );
    }

    @Override
    public void execute()
        throws Exception
    {
        String[] argsArray = args.toArray( new String[args.size()] );
        if ( runJjtree )
        {
            JJTree jjtree = new JJTree();
            int result = jjtree.main( argsArray );
            if ( result != 0 )
            {
                throw new Exception( "jjtree failed with exit code " + result );
            }
        }
        else
        {
            int result = org.javacc.parser.Main.mainProgram( argsArray );
            if ( result != 0 )
            {
                throw new Exception( "javacc failed with exit code " + result );
            }
        }
    }
}
