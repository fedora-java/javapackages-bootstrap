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
import java.util.Properties;

import javax.xml.stream.XMLStreamException;

import org.fedoraproject.mbi.model.ProjectDescriptor;

/**
 * @author Mikolaj Izdebski
 */
public class ProjectDescriptorReader
{
    public ProjectDescriptor read( String name, Properties properties, Path path )
        throws IOException, XMLStreamException
    {
        return new ProjectEntity( name, properties ).readFromXML( path );
    }
}
