/*-
 * Copyright (c) 2021-2024 Red Hat, Inc.
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

import java.io.Writer;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * An implementation of {@link XMLDumper} using Java StAX API.
 *
 * @author Mikolaj Izdebski
 */
class XMLDumperImpl implements XMLDumper {
    private static final XMLOutputFactory XML_OUTPUT_FACTORY = XMLOutputFactory.newInstance();

    private final XMLStreamWriter cursor;
    private int indent;

    XMLDumperImpl(Writer writer) throws XMLException {
        try {
            cursor = XML_OUTPUT_FACTORY.createXMLStreamWriter(writer);
        } catch (XMLStreamException e) {
            throw new XMLException(e);
        }
    }

    private void indent() throws XMLStreamException {
        for (int i = indent; i-- > 0; ) {
            cursor.writeCharacters("  ");
        }
    }

    private void newLine() throws XMLStreamException {
        cursor.writeCharacters("\n");
    }

    private void dumpStartDocument() throws XMLStreamException {
        cursor.writeStartDocument();
        newLine();
    }

    private void dumpEndDocument() throws XMLStreamException {
        cursor.writeEndDocument();
    }

    public void dumpStartElement(String tag) throws XMLException {
        try {
            indent();
            cursor.writeStartElement(tag);
        } catch (XMLStreamException e) {
            throw new XMLException(e);
        }
    }

    public void dumpEndElement() throws XMLException {
        try {
            cursor.writeEndElement();
            newLine();
        } catch (XMLStreamException e) {
            throw new XMLException(e);
        }
    }

    public void dumpText(String text) throws XMLException {
        try {
            cursor.writeCharacters(text);
        } catch (XMLStreamException e) {
            throw new XMLException(e);
        }
    }

    private <Type, Bean, Value> void doDump(Property<Type, Bean, Value> property, Type object)
            throws XMLException {
        for (Value value : property.getGetter().get(object)) {
            property.dump(this, value);
        }
    }

    public <Type, Bean> void dumpEntity(Entity<Type, Bean> entity, Type value) throws XMLException {
        try {
            dumpStartElement(entity.getTag());
            newLine();
            indent++;

            for (Property<Type, Bean, ?> property : entity.getProperties()) {
                doDump(property, value);
            }

            --indent;
            indent();
            dumpEndElement();
        } catch (XMLStreamException e) {
            throw new XMLException(e);
        }
    }

    <Type, Bean> void dumpDocument(Entity<Type, Bean> rootEntity, Type value) throws XMLException {
        try {
            dumpStartDocument();
            dumpEntity(rootEntity, value);
            dumpEndDocument();
        } catch (XMLStreamException e) {
            throw new XMLException(e);
        }
    }
}
