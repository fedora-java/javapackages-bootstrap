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
class Relationship<EnclosingType, EnclosingBean, RelatedType, RelatedBean extends Builder<RelatedType>>
    extends Constituent<EnclosingType, EnclosingBean, RelatedType, RelatedBean>
{
    private final Entity<RelatedType, RelatedBean> relatedEntity;

    public Relationship( Entity<RelatedType, RelatedBean> relatedEntity,
                         Getter<EnclosingType, Iterable<RelatedType>> getter, Setter<EnclosingBean, RelatedType> setter,
                         boolean mandatory, boolean unique )
    {
        super( relatedEntity.getTag(), getter, setter, mandatory, unique );
        this.relatedEntity = relatedEntity;
    }

    @Override
    protected void dump( XMLDumper dumper, RelatedType value )
        throws XMLStreamException
    {
        dumper.dumpEntity( relatedEntity, value );
    }

    @Override
    protected RelatedType parse( XMLParser parser )
        throws XMLStreamException
    {
        RelatedBean relatedBean = relatedEntity.newBean();
        parser.parseEntity( relatedEntity, relatedBean );
        return relatedBean.build();
    }
}