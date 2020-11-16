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
import java.util.function.Supplier;

import javax.xml.stream.XMLStreamException;

/**
 * @author Mikolaj Izdebski
 */
class Relationship<RelatedType, RelatedBean>
    extends Constituent<RelatedType, RelatedBean>
{
    private final Supplier<Entity<RelatedBean>> constructor;

    public Relationship( String tag, Consumer<RelatedType> setter, Function<RelatedBean, RelatedType> adapter,
                         boolean mandatory, boolean unique, Supplier<Entity<RelatedBean>> constructor )
    {
        super( tag, setter, adapter, mandatory, unique );
        this.constructor = constructor;
    }

    @Override
    protected RelatedBean parse( XMLParser parser )
        throws XMLStreamException
    {
        Entity<RelatedBean> relatedEntity = constructor.get();
        parser.parseEntity( relatedEntity );
        return relatedEntity.bean;
    }
}