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

import static java.util.function.Function.identity;

import java.nio.file.Paths;

import org.fedoraproject.mbi.xml.Entity;

/**
 * @author Mikolaj Izdebski
 */
class ModuleEntity
    extends Entity<ModuleBuilder>
{
    public ModuleEntity( String projectName )
    {
        super( "module", () -> new ModuleBuilder( projectName ) );
        addAttribute( "name", bean::setName, identity(), true, true );
        addAttribute( "subDir", bean::setProjectSubDir, Paths::get, true, true );
        addAttribute( "dependency", bean::addDependency, identity(), true, false );
        addCustomElement( new XBuild( bean::setExecutions ) );
    }
}