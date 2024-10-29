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
package io.kojan.xml;

import java.util.List;

/**
 * Property of data {@link Entity}. Serves as a common base class for built-in {@link Attribute}s
 * and {@link Relationship}s, as well as other user-defined custom entity properties.
 *
 * <p>An entity property is closely related to its corresponding bean property, understood as a pair
 * of {@link Getter} and {@link Setter}.
 *
 * <p>A property can be optional, meaning that no instance of the property is required. If a
 * property is not optional, then at least one instance of it is required for the entity to be
 * valid.
 *
 * <p>A property can be unique, meaning that at most one instance of the property can be present. If
 * the property is not unique, then more than one instance of the property is allowed.
 *
 * <p>Since non-unique properties allow multiple values, getters return {@link Iterable}s over
 * values and setters allow multiple calls to add multiple values.
 *
 * @param <EnclosingType> data type of entity
 * @param <EnclosingBean> type of bean associated with the entity
 * @param <NestedType> data type of property value
 * @author Mikolaj Izdebski
 */
public abstract class Property<EnclosingType, EnclosingBean, NestedType> {
    private final String tag;
    private final Getter<EnclosingType, Iterable<NestedType>> getter;
    private final Setter<EnclosingBean, NestedType> setter;
    private final boolean optional;
    private final boolean unique;

    /**
     * Initializes the abstract property.
     *
     * @param tag XML element tag name used to serialize the property in XML form (see {@link
     *     #getTag})
     * @param getter property getter method
     * @param setter property setter method
     * @param optional whether the property is optional (see {@link #isOptional})
     * @param unique whether the property is unique (see {@link #isUnique})
     */
    protected Property(
            String tag,
            Getter<EnclosingType, Iterable<NestedType>> getter,
            Setter<EnclosingBean, NestedType> setter,
            boolean optional,
            boolean unique) {
        this.tag = tag;
        this.getter = getter;
        this.setter = setter;
        this.optional = optional;
        this.unique = unique;
    }

    /**
     * Serializes the property into XML format, using given {@link XMLDumper}.
     *
     * @param dumper the sink to serialize data to
     * @param value property value to serialize
     * @throws XMLException in case exception occurs during XML serialization
     */
    protected abstract void dump(XMLDumper dumper, NestedType value) throws XMLException;

    /**
     * Deserializes the property from XML format, using given {@link XMLParser}.
     *
     * @param parser the source to deserialize data from
     * @return deserialized property value
     * @throws XMLException in case exception occurs during XML deserialization
     */
    protected abstract NestedType parse(XMLParser parser) throws XMLException;

    /**
     * Determines XML element tag name used to serialize the property in XML form.
     *
     * @return XML element tag name
     */
    public String getTag() {
        return tag;
    }

    /**
     * Determines whether the property is optional or not.
     *
     * <p>A property can be optional, meaning that no instance of the property is required. If a
     * property is not optional, then at least one instance of it is required for the entity to be
     * valid.
     *
     * @return {@code true} iff the property is optional
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * Determines whether the property is unique or not.
     *
     * <p>A property can be unique, meaning that at most one instance of the property can be
     * present. If the property is not unique, then more than one instance of the property is
     * allowed.
     *
     * @return {@code true} iff the property is unique
     */
    public boolean isUnique() {
        return unique;
    }

    /**
     * Obtain property getter method that can be used to retrieve property value.
     *
     * @return property getter method
     */
    public Getter<EnclosingType, Iterable<NestedType>> getGetter() {
        return getter;
    }

    /**
     * Obtain property setter method that can be used to update property value.
     *
     * @return property setter method
     */
    public Setter<EnclosingBean, NestedType> getSetter() {
        return setter;
    }

    static <T> Iterable<T> singleton(T t) {
        return t != null ? List.of(t) : List.of();
    }
}
