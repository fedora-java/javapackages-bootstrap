/*-
 * Copyright (c) 2020-2023 Red Hat, Inc.
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
package org.fedoraproject.mbi.tool.sisu;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.fedoraproject.mbi.tool.ProjectClassScope;
import org.fedoraproject.mbi.tool.Tool;

/**
 * @author Mikolaj Izdebski
 */
@ProjectClassScope
public class SisuTool
    extends Tool
{
    private static final String SISU_INDEX_PATH = "META-INF/sisu/javax.inject.Named";

    private final Set<String> namedComponents = new TreeSet<>();

    private void gleanFromClasses()
        throws Exception
    {
        for ( Path classFile : Files.walk( getClassesDir() ).filter( Files::isRegularFile ).filter( path -> path.toString().endsWith( ".class" ) ).collect( Collectors.toList() ) )
        {
            String className =
                getClassesDir().relativize( classFile ).toString().replaceAll( ".class$", "" ).replace( '/', '.' );
            Class<?> cls = Thread.currentThread().getContextClassLoader().loadClass( className );
            if ( cls.isAnnotationPresent( Named.class ) )
            {
                namedComponents.add( cls.getName() );
            }
        }
    }

    @Override
    public void execute()
        throws Exception
    {
        gleanFromClasses();

        if ( namedComponents.isEmpty() )
        {
            throw new RuntimeException( "No JSR-330 components were discovered for module " + getModule().getName() );
        }

        Path indexPath = getClassesDir().resolve( SISU_INDEX_PATH );
        Files.createDirectories( indexPath.getParent() );
        try ( PrintWriter pw = new PrintWriter( Files.newBufferedWriter( indexPath ) ) )
        {
            for ( String component : namedComponents )
            {
                pw.println( component );
            }
        }
    }
}
