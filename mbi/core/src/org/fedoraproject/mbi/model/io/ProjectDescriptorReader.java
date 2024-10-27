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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.fedoraproject.mbi.model.LicensingDescriptor;
import org.fedoraproject.mbi.model.ModuleDescriptor;
import org.fedoraproject.mbi.model.ProjectDescriptor;

import io.kojan.xml.Attribute;
import io.kojan.xml.Entity;
import io.kojan.xml.Relationship;
import io.kojan.xml.XMLException;

/**
 * @author Mikolaj Izdebski
 */
public class ProjectDescriptorReader
{
    public ProjectDescriptor read( String name, Properties properties, Path path )
        throws IOException, XMLException
    {
        var licensingEntity =
            Entity.of( "licensing", LicensingBuilder::new,
                       Attribute.of( "tag", LicensingDescriptor::getTag, LicensingBuilder::setTag ),
                       Attribute.ofMulti( "file", LicensingDescriptor::getFiles, LicensingBuilder::addFile ),
                       Attribute.ofOptional( "text", LicensingDescriptor::getText, LicensingBuilder::setText ) );

        var moduleEntity =
            Entity.of( "module", () -> new ModuleBuilder( name ),
                       Attribute.ofOptional( "name", ModuleDescriptor::getName, ModuleBuilder::setName ),
                       Attribute.ofOptional( "subDir", ModuleDescriptor::getProjectSubDir,
                                             ModuleBuilder::setProjectSubDir, Path::toString, Paths::get ),
                       Attribute.ofMulti( "dependency", ModuleDescriptor::getDependencies,
                                          ModuleBuilder::addDependency ),
                       new XBuild( ModuleDescriptor::getExecutions, ModuleBuilder::setExecutions ) );

        var projectEntity =
            Entity.of( "project", () -> new ProjectBuilder( name, properties ),
                       Relationship.ofSingular( licensingEntity, ProjectDescriptor::getLicensing,
                                                ProjectBuilder::setLicensing ),
                       Relationship.of( moduleEntity, ProjectDescriptor::getModules, ProjectBuilder::addModule ) );

        return projectEntity.readFromXML( path );
    }
}
