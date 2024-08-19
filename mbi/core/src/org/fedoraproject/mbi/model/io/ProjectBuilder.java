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

import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import org.fedoraproject.mbi.model.LicensingDescriptor;
import org.fedoraproject.mbi.model.ModuleDescriptor;
import org.fedoraproject.mbi.model.ProjectDescriptor;

import io.kojan.xml.Builder;

/**
 * @author Mikolaj Izdebski
 */
class ProjectBuilder
    implements Builder<ProjectDescriptor>
{
    private final String name;

    private final Properties properties;

    private LicensingDescriptor licensing;

    private final Set<ModuleDescriptor> modules = new LinkedHashSet<>();

    public ProjectBuilder( String name, Properties properties )
    {
        this.name = name;
        this.properties = properties;
    }

    public void setLicensing( LicensingDescriptor licensing )
    {
        this.licensing = licensing;
    }

    public void addModule( ModuleDescriptor module )
    {
        modules.add( module );
    }

    @Override
    public ProjectDescriptor build()
    {
        return new ProjectDescriptor( name, properties, licensing, modules );
    }
}
