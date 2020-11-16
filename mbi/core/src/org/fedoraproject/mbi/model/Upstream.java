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
package org.fedoraproject.mbi.model;

/**
 * @author Mikolaj Izdebski
 */
public class Upstream
{
    private final String url;

    private final String ref;

    private final String version;

    public Upstream( String url, String ref, String version )
    {
        String[] tildeParts = version.split( "\\~" );
        String[] dotParts = tildeParts[0].split( "\\." );
        if ( ref.contains( "@@" ) )
        {
            ref = ref.replaceAll( "@@", tildeParts[1] );
        }
        for ( int i = 0; ref.contains( "@" ); i++ )
        {
            ref = ref.replaceFirst( "@", dotParts[i] );
        }
        this.url = url;
        this.ref = ref;
        this.version = version;
    }

    public String getUrl()
    {
        return url;
    }

    public String getRef()
    {
        return ref;
    }

    public String getVersion()
    {
        return version;
    }
}
