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

import javax.xml.stream.XMLStreamException;

/**
 * @author Mikolaj Izdebski
 */
public abstract class Constituent<EnclosingType, EnclosingBean, NestedType, NestedBean>
{
    private final String tag;

    private final Getter<EnclosingType, Iterable<NestedType>> getter;

    private final Setter<EnclosingBean, NestedType> setter;

    private final boolean optional;

    private final boolean unique;

    public Constituent( String tag, Getter<EnclosingType, Iterable<NestedType>> getter,
                        Setter<EnclosingBean, NestedType> setter, boolean optional, boolean unique )
    {
        this.tag = tag;
        this.getter = getter;
        this.setter = setter;
        this.optional = optional;
        this.unique = unique;
    }

    protected abstract void dump( XMLDumper dumper, NestedType value )
        throws XMLStreamException;

    protected abstract NestedType parse( XMLParser parser )
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

    public void doDump( XMLDumper dumper, EnclosingType object )
        throws XMLStreamException
    {
        for ( NestedType value : getter.get( object ) )
        {
            dump( dumper, value );
        }
    }

    public boolean tryParse( XMLParser parser, EnclosingBean bean )
        throws XMLStreamException
    {
        if ( parser.hasStartElement( tag ) )
        {
            setter.set( bean, parse( parser ) );
            return true;
        }

        return false;
    }
}
