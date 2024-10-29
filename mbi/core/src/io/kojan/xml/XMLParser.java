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

/**
 * A facility to deserialize data in in XML format. Allows deserialization of entities and reading
 * of any other data.
 *
 * @author Mikolaj Izdebski
 */
public interface XMLParser {
    /**
     * Reads XML text content.
     *
     * <p>If there is no XML text content at given parser position, then empty String is returned.
     *
     * <p>Comments within the text are skipped.
     *
     * @return text content that was read
     * @throws XMLException in case exception occurs during XML deserialization
     */
    String parseText() throws XMLException;

    /**
     * Determines whether at the current parser position there is an XML element opening tag.
     *
     * <p>Comments and whitespace text preceding the XML tag are skipped.
     *
     * @return true iff at the current parser position there is an XML element
     * @throws XMLException in case exception occurs during XML deserialization
     */
    boolean hasStartElement() throws XMLException;

    /**
     * Determines whether at the current parser position there is an XML element opening tag with
     * specified tag name.
     *
     * <p>Comments and whitespace text preceding the XML tag are skipped.
     *
     * @param tag XML element tag name
     * @return true iff at the current parser position there is an XML element with specified tag
     *     name
     * @throws XMLException in case exception occurs during XML deserialization
     */
    boolean hasStartElement(String tag) throws XMLException;

    /**
     * Reads XML element opening tag.
     *
     * <p>Comments and whitespace text preceding the XML tag are skipped.
     *
     * <p>{@link XMLException} is thrown if at the current position there is no XML element (but,
     * for example, text data or element closing tag).
     *
     * @return XML element tag name
     * @throws XMLException in case exception occurs during XML deserialization
     */
    String parseStartElement() throws XMLException;

    /**
     * Reads XML element opening tag with specified tag name.
     *
     * <p>Comments and whitespace text preceding the XML tag are skipped.
     *
     * <p>{@link XMLException} is thrown if at the current position there is no XML element opening
     * tag with specified tag (but, for example, text data, element closing tag or element opening
     * tag with a different tag name).
     *
     * @param tag XML element tag name
     * @throws XMLException in case exception occurs during XML deserialization
     */
    void parseStartElement(String tag) throws XMLException;

    /**
     * Reads XML element closing tag with specified tag name.
     *
     * <p>Comments and whitespace text preceding the XML tag are skipped.
     *
     * <p>{@link XMLException} is thrown if at the current position there is no XML element closing
     * tag with specified tag (but, for example, text data, element opening tag or XML element
     * closing tag with a different tag name).
     *
     * @param tag XML element tag name
     * @throws XMLException in case exception occurs during XML deserialization
     */
    void parseEndElement(String tag) throws XMLException;

    /**
     * Deserializes given {@link Entity} from XML form.
     *
     * @param <Type> data type of entity
     * @param <Bean> type of bean associated with the entity
     * @param entity the entity type to deserialize
     * @param bean the entity bean to deserialize data into
     * @throws XMLException in case exception occurs during XML deserialization
     */
    <Type, Bean> void parseEntity(Entity<Type, Bean> entity, Bean bean) throws XMLException;
}
