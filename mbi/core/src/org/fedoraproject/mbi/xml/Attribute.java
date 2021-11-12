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

import java.util.function.Function;

import javax.xml.stream.XMLStreamException;

/**
 * @author Mikolaj Izdebski
 */
class Attribute<EnclosingType, EnclosingBean, AttributeType>
    extends Constituent<EnclosingType, EnclosingBean, AttributeType, String>
{
    private final Function<AttributeType, String> toStringAdapter;

    private final Function<String, AttributeType> fromStringAdapter;

    public Attribute( String tag, Getter<EnclosingType, Iterable<AttributeType>> getter,
                      Setter<EnclosingBean, AttributeType> setter, Function<AttributeType, String> toStringAdapter,
                      Function<String, AttributeType> fromStringAdapter, boolean optional, boolean unique )
    {
        super( tag, getter, setter, optional, unique );
        this.toStringAdapter = toStringAdapter;
        this.fromStringAdapter = fromStringAdapter;
    }

    @Override
    protected void dump( XMLDumper dumper, AttributeType value )
        throws XMLStreamException
    {
        dumper.dumpStartElement( getTag() );
        dumper.dumpText( toStringAdapter.apply( value ) );
        dumper.dumpEndElement();
    }

    @Override
    protected AttributeType parse( XMLParser parser )
        throws XMLStreamException
    {
        parser.parseStartElement( getTag() );
        String text = parser.parseText();
        parser.parseEndElement( getTag() );
        return fromStringAdapter.apply( text );
    }
}