/*-
 * Copyright (c) 2020-2024 Red Hat, Inc.
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
package io.kojan.xml;

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
 * An implementation of {@link XMLParser} using Java StAX API.
 *
 * @author Mikolaj Izdebski
 */
class XMLParserImpl implements XMLParser {
    private static final XMLInputFactory XML_INPUT_FACTORY = XMLInputFactory.newInstance();
    private final XMLStreamReader cursor;

    public XMLParserImpl(Reader reader) throws XMLException {
        try {
            cursor = XML_INPUT_FACTORY.createXMLStreamReader(reader);
        } catch (XMLStreamException e) {
            throw new XMLException(e);
        }
    }

    private XMLException error(String message) throws XMLException {
        return new XMLException(
                message
                        + ", line: "
                        + cursor.getLocation().getLineNumber()
                        + ", columnn:"
                        + cursor.getLocation().getColumnNumber());
    }

    public String parseText() throws XMLException {
        try {
            for (StringBuilder sb = new StringBuilder(); ; cursor.next()) {
                if (cursor.getEventType() == CHARACTERS) {
                    sb.append(cursor.getText());
                } else if (cursor.getEventType() != COMMENT) {
                    return sb.toString();
                }
            }
        } catch (XMLStreamException e) {
            throw new XMLException(e);
        }
    }

    private void skipWhiteSpace() throws XMLException {
        if (!parseText().chars().allMatch(Character::isWhitespace)) {
            throw error("Expected white space");
        }
    }

    public boolean hasStartElement() throws XMLException {
        skipWhiteSpace();

        return cursor.getEventType() == START_ELEMENT;
    }

    public boolean hasStartElement(String tag) throws XMLException {
        return hasStartElement() && cursor.getLocalName().equals(tag);
    }

    public String parseStartElement() throws XMLException {
        try {
            if (!hasStartElement()) {
                throw error("Expected a start element");
            }
            String tag = cursor.getLocalName();
            cursor.next();
            return tag;
        } catch (XMLStreamException e) {
            throw new XMLException(e);
        }
    }

    public void parseStartElement(String tag) throws XMLException {
        try {
            if (!hasStartElement(tag)) {
                throw error("Expected <" + tag + "> start element");
            }
            cursor.next();
        } catch (XMLStreamException e) {
            throw new XMLException(e);
        }
    }

    private void expectToken(int token, String description) throws XMLException {
        skipWhiteSpace();

        if (cursor.getEventType() != token) {
            throw error("Expected " + description);
        }
    }

    public void parseEndElement(String tag) throws XMLException {
        try {
            expectToken(END_ELEMENT, "</" + tag + "> end element");
            cursor.next();
        } catch (XMLStreamException e) {
            throw new XMLException(e);
        }
    }

    private void parseStartDocument() throws XMLException {
        expectToken(START_DOCUMENT, "start of document");
        try {
            cursor.next();
        } catch (XMLStreamException e) {
            throw new XMLException(e);
        }
    }

    private void parseEndDocument() throws XMLException {
        expectToken(END_DOCUMENT, "end of document");
    }

    private <Type, Bean, Value> boolean tryParse(Property<Type, Bean, Value> property, Bean bean)
            throws XMLException {
        if (hasStartElement(property.getTag())) {
            property.getSetter().set(bean, property.parse(this));
            return true;
        }

        return false;
    }

    public <Type, Bean> void parseEntity(Entity<Type, Bean> entity, Bean bean) throws XMLException {
        parseStartElement(entity.getTag());

        Set<Property<Type, Bean, ?>> allowedProperties =
                new LinkedHashSet<>(entity.getProperties());

        for (Iterator<Property<Type, Bean, ?>> iterator = allowedProperties.iterator();
                iterator.hasNext(); ) {
            Property<Type, Bean, ?> property = iterator.next();

            if (tryParse(property, bean)) {
                if (property.isUnique()) {
                    iterator.remove();
                }

                iterator = allowedProperties.iterator();
            }
        }

        parseEndElement(entity.getTag());

        for (Property<Type, Bean, ?> property : allowedProperties) {
            if (!property.isOptional()) {
                throw error(
                        "Mandatory <"
                                + property.getTag()
                                + "> property of <"
                                + entity.getTag()
                                + "> has not been set");
            }
        }
    }

    <Type, Bean> Type parseDocument(Entity<Type, Bean> rootEntity) throws XMLException {
        Bean rootBean = rootEntity.getBeanFactory().newInstance();
        parseStartDocument();
        parseEntity(rootEntity, rootBean);
        parseEndDocument();
        return rootEntity.getBeanConverter().convert(rootBean);
    }
}
