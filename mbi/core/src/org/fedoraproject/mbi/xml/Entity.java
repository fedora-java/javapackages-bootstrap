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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.xml.stream.XMLStreamException;

/**
 * @author Mikolaj Izdebski
 */
public class Entity<Type, Bean extends Builder<Type>>
{
    private final String tag;

    private final Factory<Bean> beanFactory;

    private final List<Constituent<Type, Bean, ?, ?>> elements = new ArrayList<>();

    public Entity( String tag, Factory<Bean> beanFactory )
    {
        this.tag = tag;
        this.beanFactory = beanFactory;
    }

    public String getTag()
    {
        return tag;
    }

    public Bean newBean()
    {
        return beanFactory.newInstance();
    }

    public List<Constituent<Type, Bean, ?, ?>> getElements()
    {
        return Collections.unmodifiableList( elements );
    }

    public void addAttribute( String tag, Getter<Type, String> getter, Setter<Bean, String> setter )
    {
        elements.add( new Attribute<>( tag, new GetterAdapter<>( getter ), setter, Function.identity(),
                                       Function.identity(), false, true ) );
    }

    public <AttributeType> void addAttribute( String tag, Getter<Type, AttributeType> getter,
                                              Setter<Bean, AttributeType> setter,
                                              Function<AttributeType, String> toStringAdapter,
                                              Function<String, AttributeType> fromStringAdapter )
    {
        elements.add( new Attribute<>( tag, new GetterAdapter<>( getter ), setter, toStringAdapter, fromStringAdapter,
                                       false, true ) );
    }

    public void addOptionalAttribute( String tag, Getter<Type, String> getter, Setter<Bean, String> setter )
    {
        elements.add( new Attribute<>( tag, new GetterAdapter<>( getter ), setter, Function.identity(),
                                       Function.identity(), true, true ) );
    }

    public <AttributeType> void addOptionalAttribute( String tag, Getter<Type, AttributeType> getter,
                                                      Setter<Bean, AttributeType> setter,
                                                      Function<AttributeType, String> toStringAdapter,
                                                      Function<String, AttributeType> fromStringAdapter )
    {
        elements.add( new Attribute<>( tag, new GetterAdapter<>( getter ), setter, toStringAdapter, fromStringAdapter,
                                       true, true ) );
    }

    public void addMultiAttribute( String tag, Getter<Type, Iterable<String>> getter, Setter<Bean, String> setter )
    {
        elements.add( new Attribute<>( tag, getter, setter, Function.identity(), Function.identity(), true, false ) );
    }

    public <AttributeType> void addMultiAttribute( String tag, Getter<Type, Iterable<AttributeType>> getter,
                                                   Setter<Bean, AttributeType> setter,
                                                   Function<AttributeType, String> toStringAdapter,
                                                   Function<String, AttributeType> fromStringAdapter )
    {
        elements.add( new Attribute<>( tag, getter, setter, toStringAdapter, fromStringAdapter, true, false ) );
    }

    public <RelatedType, RelatedBean extends Builder<RelatedType>> void addSingularRelationship( Entity<RelatedType, RelatedBean> relatedEntity,
                                                                                                 Getter<Type, RelatedType> getter,
                                                                                                 Setter<Bean, RelatedType> setter )
    {
        elements.add( new Relationship<>( relatedEntity, new GetterAdapter<>( getter ), setter, true, true ) );
    }

    public <RelatedType, RelatedBean extends Builder<RelatedType>> void addRelationship( Entity<RelatedType, RelatedBean> relatedEntity,
                                                                                         Getter<Type, Iterable<RelatedType>> getter,
                                                                                         Setter<Bean, RelatedType> setter )
    {
        elements.add( new Relationship<>( relatedEntity, getter, setter, true, false ) );
    }

    public void addCustomElement( Constituent<Type, Bean, ?, ?> element )
    {
        elements.add( element );
    }

    public Type readFromXML( Reader reader )
        throws IOException, XMLStreamException
    {
        XMLParser parser = new XMLParser( reader );
        return parser.parseDocument( this );
    }

    public Type readFromXML( Path path )
        throws IOException, XMLStreamException
    {
        try ( Reader reader = Files.newBufferedReader( path ) )
        {
            return readFromXML( reader );
        }
    }

    public void writeToXML( Path path, Type object )
        throws IOException, XMLStreamException
    {
        try ( Writer writer = Files.newBufferedWriter( path ) )
        {
            XMLDumper dumper = new XMLDumper( writer );
            dumper.dumpDocument( this, object );
        }
    }
}