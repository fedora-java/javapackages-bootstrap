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

/**
 * A facility to serialize data in in XML format. Allows serialization of entities and writing of
 * any other data.
 *
 * @author Mikolaj Izdebski
 */
public interface XMLDumper {
    /**
     * Writes a sequence that starts an XML element with given tag.
     *
     * @param tag element tag name
     * @throws XMLException in case exception occurs during XML serialization
     */
    void dumpStartElement(String tag) throws XMLException;

    /**
     * Writes a sequence that ends previously started XML element.
     *
     * @throws XMLException in case exception occurs during XML serialization
     */
    void dumpEndElement() throws XMLException;

    /**
     * Writes XML text content.
     *
     * @param text text content to write
     * @throws XMLException in case exception occurs during XML serialization
     */
    void dumpText(String text) throws XMLException;

    /**
     * Serializes given {@link Entity} into XML form.
     *
     * @param <Type> data type of entity
     * @param <Bean> type of bean associated with the entity
     * @param entity the entity type to serialize
     * @param value the object to serialize
     * @throws XMLException in case exception occurs during XML serialization
     */
    <Type, Bean> void dumpEntity(Entity<Type, Bean> entity, Type value) throws XMLException;
}
