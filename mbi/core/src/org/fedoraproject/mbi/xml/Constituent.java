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
public abstract class Constituent<NestedType, NestedBean>
{
    private final String tag;

    private final Consumer<NestedType> setter;

    private final Function<NestedBean, NestedType> adapter;

    private final boolean optional;

    private final boolean unique;

    public Constituent( String tag, Consumer<NestedType> setter, Function<NestedBean, NestedType> adapter,
                        boolean optional, boolean unique )
    {
        this.tag = tag;
        this.setter = setter;
        this.adapter = adapter;
        this.optional = optional;
        this.unique = unique;
    }

    protected abstract NestedBean parse( XMLParser parser )
        throws XMLStreamException;

    public String getTag()
    {
        return tag;
    }

    public boolean isOptional()
    {
        return optional;
    }

    public boolean isUnique()
    {
        return unique;
    }

    public boolean tryParse( XMLParser parser )
        throws XMLStreamException
    {
        if ( parser.hasStartElement( tag ) )
        {
            setter.accept( adapter.apply( parse( parser ) ) );
            return true;
        }

        return false;
    }
}
