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
package org.fedoraproject.mbi;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Mikolaj Izdebski
 */
public class Util
{
    public static void filterJavaSources( List<Path> included, List<Path> excluded, Path sourceRoot,
                                          Predicate<Path> filter )
        throws IOException
    {
        Files.walk( sourceRoot,
                    FileVisitOption.FOLLOW_LINKS ).filter( Files::isRegularFile ).filter( path -> path.toString().endsWith( ".java" ) ).forEachOrdered( path -> ( filter.test( path ) ? included : excluded ).add( path ) );
    }

    public static List<Path> findJavaSources( Path sourceRoot )
        throws IOException
    {
        List<Path> included = new ArrayList<>();
        filterJavaSources( included, new ArrayList<>(), sourceRoot, path -> true );
        return included;
    }

    public static void delete( Path path )
        throws IOException
    {
        if ( Files.isDirectory( path, LinkOption.NOFOLLOW_LINKS ) )
        {
            List<Path> paths = new ArrayList<>();
            try ( DirectoryStream<Path> stream = Files.newDirectoryStream( path ) )
            {
                for ( Path dirEntry : stream )
                {
                    paths.add( dirEntry );
                }
            }
            for ( Path child : paths )
            {
                delete( child );
            }
        }

        if ( Files.exists( path, LinkOption.NOFOLLOW_LINKS ) )
        {
            Files.delete( path );
        }
    }

    public static void copy( Path srcDir, Path destDir, Predicate<? super Path> filter )
        throws IOException
    {
        for ( Path srcFile : Files.walk( srcDir ).filter( Files::isRegularFile ).filter( filter ).collect( Collectors.toList() ) )
        {
            Path destFile = destDir.resolve( srcDir.relativize( srcFile ) );
            Files.createDirectories( destFile.getParent() );
            try ( var inputStream = Files.newInputStream( srcFile );
                            var outputStream = Files.newOutputStream( destFile, StandardOpenOption.CREATE_NEW ) )
            {
                inputStream.transferTo( outputStream );
            }
        }
    }
}
