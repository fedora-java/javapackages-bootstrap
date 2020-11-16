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
package org.fedoraproject.mbi.xml;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.xml.stream.XMLStreamException;

/**
 * @author Mikolaj Izdebski
 */
class Attribute<AttributeType>
    extends Constituent<AttributeType, String>
{
    public Attribute( String tag, Consumer<AttributeType> setter, Function<String, AttributeType> adapter,
                      boolean optional, boolean unique )
    {
        super( tag, setter, adapter, optional, unique );
    }

    @Override
    protected String parse( XMLParser parser )
        throws XMLStreamException
    {
        parser.parseStartElement( getTag() );
        String text = parser.parseText();
        parser.parseEndElement( getTag() );
        return text;
    }
}