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

import java.util.Collections;
import java.util.Properties;
import java.util.Set;

/**
 * @author Mikolaj Izdebski
 */
public class ProjectDescriptor
{
    private final String name;

    private final Properties properties;

    private final LicensingDescriptor licensing;

    private final Set<ModuleDescriptor> modules;

    public ProjectDescriptor( String name, Properties properties, LicensingDescriptor licensing,
                              Set<ModuleDescriptor> modules )
    {
        this.name = name;
        this.properties = properties;
        this.licensing = licensing;
        this.modules = Collections.unmodifiableSet( modules );
    }

    public String getName()
    {
        return name;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public LicensingDescriptor getLicensing()
    {
        return licensing;
    }

    public Set<ModuleDescriptor> getModules()
    {
        return modules;
    }

    public String getMBIVersion()
    {
        if ( name.equals( "mbi" ) )
        {
            return "MBI";
        }
        return getProperties().getProperty( "version" ).replaceAll( "[^0-9A-Za-z.]", "." ) + ".MBI";
    }
}