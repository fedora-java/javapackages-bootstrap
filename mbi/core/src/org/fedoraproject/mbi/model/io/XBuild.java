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

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.fedoraproject.mbi.model.Execution;
import org.fedoraproject.mbi.model.Instruction;
import org.fedoraproject.mbi.model.ModuleDescriptor;
import org.fedoraproject.mbi.xml.Constituent;
import org.fedoraproject.mbi.xml.Getter;
import org.fedoraproject.mbi.xml.GetterAdapter;
import org.fedoraproject.mbi.xml.Setter;
import org.fedoraproject.mbi.xml.XMLDumper;
import org.fedoraproject.mbi.xml.XMLParser;

/**
 * @author Mikolaj Izdebski
 */
class XBuild
    extends Constituent<ModuleDescriptor, ModuleBuilder, List<Execution>, List<Execution>>
{
    public XBuild( Getter<ModuleDescriptor, List<Execution>> getter, Setter<ModuleBuilder, List<Execution>> setter )
    {
        super( "build", new GetterAdapter<>( getter ), setter, false, true );
    }

    @Override
    protected List<Execution> parse( XMLParser parser )
        throws XMLStreamException
    {
        List<Execution> executions = new ArrayList<>();
        parser.parseStartElement( getTag() );
        while ( parser.hasStartElement() )
        {
            executions.add( parseExecution( parser ) );
        }
        parser.parseEndElement( getTag() );
        return executions;
    }

    private Execution parseExecution( XMLParser parser )
        throws XMLStreamException
    {
        List<Instruction> instructions = new ArrayList<>();
        String toolName = parser.parseStartElement();
        while ( parser.hasStartElement() )
        {
            String methodName = parser.parseStartElement();
            String argument = parser.parseText();
            parser.parseEndElement( methodName );
            instructions.add( new Instruction( methodName, argument ) );
        }
        parser.parseEndElement( toolName );
        return new Execution( toolName, instructions );
    }

    @Override
    protected void dump( XMLDumper dumper, List<Execution> value )
        throws XMLStreamException
    {
        throw new IllegalStateException( "Dumping is not implemented" );
    }
}