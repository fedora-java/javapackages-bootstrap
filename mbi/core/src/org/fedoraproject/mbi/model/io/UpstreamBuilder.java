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
package org.fedoraproject.mbi.model.io;

import org.fedoraproject.mbi.model.Upstream;

/**
 * @author Mikolaj Izdebski
 */
public class UpstreamBuilder
{
    private String url;

    private String ref;

    private String version;

    public void setUrl( String url )
    {
        this.url = url;
    }

    public void setRef( String ref )
    {
        this.ref = ref;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public Upstream build()
    {
        return new Upstream( url, ref, version );
    }
}
