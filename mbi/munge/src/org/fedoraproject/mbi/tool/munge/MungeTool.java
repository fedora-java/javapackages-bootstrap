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
package org.fedoraproject.mbi.tool.munge;

import java.nio.file.Files;
import java.nio.file.Path;

import org.fedoraproject.mbi.Util;
import org.fedoraproject.mbi.tool.Instruction;
import org.fedoraproject.mbi.tool.Tool;
import org.sonatype.plugins.munge.Munge;

/**
 * @author Mikolaj Izdebski
 */
public class MungeTool
    extends Tool
{
    private String sourceRoot;

    @Instruction
    public void setSourceRoot( String sourceRoot )
    {
        this.sourceRoot = sourceRoot;
    }

    @Override
    public void execute()
        throws Exception
    {
        Path srcDir = getSourceRootDir().resolve( sourceRoot );
        Path destDir = getGeneratedSourcesDir();
        for ( Path src : Util.findJavaSources( srcDir ) )
        {
            Path dest = destDir.resolve( srcDir.relativize( src ) );
            Files.createDirectories( dest.getParent() );
            String[] args = { "-DNO_AOP", src.toString(), dest.toString() };
            Munge.main( args );
        }
    }
}
