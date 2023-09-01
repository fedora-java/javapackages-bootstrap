/*-
 * Copyright (c) 2021 Red Hat, Inc.
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
package org.fedoraproject.mbi.xml;

import java.io.Writer;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author Mikolaj Izdebski
 */
public class XMLDumper
{
    private static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newInstance();

    private final XMLStreamWriter cursor;

    private int indent;

    public XMLDumper( Writer writer )
        throws XMLStreamException
    {
        cursor = XML_OUTPUT_FACTORY.createXMLStreamWriter( writer );
    }

    private void indent()
        throws XMLStreamException
    {
        for ( int i = indent; i-- > 0; )
        {
            cursor.writeCharacters( "  " );
        }
    }

    private void newLine()
        throws XMLStreamException
    {
        cursor.writeCharacters( "\n" );
    }

    public void dumpStartDocument()
        throws XMLStreamException
    {
        cursor.writeStartDocument();
        newLine();
    }

    public void dumpEndDocument()
        throws XMLStreamException
    {
        cursor.writeEndDocument();
    }

    public void dumpStartElement( String tag )
        throws XMLStreamException
    {
        indent();
        cursor.writeStartElement( tag );
    }

    public void dumpEndElement()
        throws XMLStreamException
    {
        cursor.writeEndElement();
        newLine();
    }

    public void dumpText( String text )
        throws XMLStreamException
    {
        cursor.writeCharacters( text );
    }

    public <Type, Bean extends Builder<Type>> void dumpEntity( Entity<Type, Bean> entity, Type value )
        throws XMLStreamException
    {
        dumpStartElement( entity.getTag() );
        newLine();
        indent++;

        for ( Constituent<Type, Bean, ?, ?> constituent : entity.getElements() )
        {
            constituent.doDump( this, value );
        }

        --indent;
        indent();
        dumpEndElement();
    }

    public <Type, Bean extends Builder<Type>> void dumpDocument( Entity<Type, Bean> rootEntity, Type value )
        throws XMLStreamException
    {
        dumpStartDocument();
        dumpEntity( rootEntity, value );
        dumpEndDocument();
    }
}
