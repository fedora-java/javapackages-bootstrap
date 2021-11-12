/*-
 * Copyright (c) 2020-2021 Red Hat, Inc.
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

import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.COMMENT;
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.io.Reader;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * @author Mikolaj Izdebski
 */
public class XMLParser
{
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();

    private final XMLStreamReader cursor;

    public XMLParser( Reader reader )
        throws XMLStreamException
    {
        cursor = XML_INPUT_FACTORY.createXMLStreamReader( reader );
    }

    private void error( String message )
        throws XMLStreamException
    {
        throw new XMLStreamException( message + ", line: " + cursor.getLocation().getLineNumber() + ", columnn:"
            + cursor.getLocation().getColumnNumber() );
    }

    public String parseText()
        throws XMLStreamException
    {
        for ( StringBuilder sb = new StringBuilder();; cursor.next() )
        {
            if ( cursor.getEventType() == CHARACTERS )
            {
                sb.append( cursor.getText() );
            }
            else if ( cursor.getEventType() != COMMENT )
            {
                return sb.toString();
            }
        }
    }

    private void skipWhiteSpace()
        throws XMLStreamException
    {
        if ( !parseText().chars().allMatch( Character::isWhitespace ) )
        {
            error( "Expected white space" );
        }
    }

    public boolean hasStartElement()
        throws XMLStreamException
    {
        skipWhiteSpace();

        return cursor.getEventType() == START_ELEMENT;
    }

    public boolean hasStartElement( String tag )
        throws XMLStreamException
    {
        return hasStartElement() && cursor.getLocalName().equals( tag );
    }

    public String parseStartElement()
        throws XMLStreamException
    {
        if ( !hasStartElement() )
        {
            error( "Expected a start element" );
        }

        String tag = cursor.getLocalName();

        cursor.next();

        return tag;
    }

    public void parseStartElement( String tag )
        throws XMLStreamException
    {
        if ( !hasStartElement( tag ) )
        {
            error( "Expected <" + tag + "> start element" );
        }

        cursor.next();
    }

    private void expectToken( int token, String description )
        throws XMLStreamException
    {
        skipWhiteSpace();

        if ( cursor.getEventType() != token )
        {
            error( "Expected " + description );
        }
    }

    public void parseEndElement( String tag )
        throws XMLStreamException
    {
        expectToken( END_ELEMENT, "</" + tag + "> end element" );
        cursor.next();
    }

    public void parseStartDocument()
        throws XMLStreamException
    {
        expectToken( START_DOCUMENT, "start of document" );
        cursor.next();
    }

    public void parseEndDocument()
        throws XMLStreamException
    {
        expectToken( END_DOCUMENT, "end of document" );
    }

    public <Type, Bean extends Builder<Type>> void parseEntity( Entity<Type, Bean> entity, Bean bean )
        throws XMLStreamException
    {
        parseStartElement( entity.getTag() );

        Set<Constituent<Type, Bean, ?, ?>> allowedElements = new LinkedHashSet<>( entity.getElements() );

        for ( Iterator<Constituent<Type, Bean, ?, ?>> iterator = allowedElements.iterator(); iterator.hasNext(); )
        {
            Constituent<Type, Bean, ?, ?> constituent = iterator.next();

            if ( constituent.tryParse( this, bean ) )
            {
                if ( constituent.isUnique() )
                {
                    iterator.remove();
                }

                iterator = allowedElements.iterator();
            }
        }

        parseEndElement( entity.getTag() );

        for ( Constituent<Type, Bean, ?, ?> constituent : allowedElements )
        {
            if ( !constituent.isOptional() )
            {
                error( "Mandatory <" + constituent.getTag() + "> of <" + entity.getTag() + "> has not been set" );
            }
        }
    }

    public <Type, Bean extends Builder<Type>> Type parseDocument( Entity<Type, Bean> rootEntity )
        throws XMLStreamException
    {
        Bean rootBean = rootEntity.newBean();
        parseStartDocument();
        parseEntity( rootEntity, rootBean );
        parseEndDocument();
        return rootBean.build();
    }
}
