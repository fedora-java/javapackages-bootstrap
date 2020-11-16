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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Mikolaj Izdebski
 */
public class Entity<Bean>
{
    private final String tag;

    public final Bean bean;

    private final List<Constituent<?, ?>> elements = new ArrayList<>();

    public Entity( String tag, Supplier<Bean> constructor )
    {
        this.tag = tag;
        this.bean = constructor.get();
    }

    public String getTag()
    {
        return tag;
    }

    public List<Constituent<?, ?>> getElements()
    {
        return Collections.unmodifiableList( elements );
    }

    protected <AttributeType> void addAttribute( String tag, Consumer<AttributeType> setter,
                                                 Function<String, AttributeType> adapter, boolean optional,
                                                 boolean unique )
    {
        elements.add( new Attribute<>( tag, setter, adapter, optional, unique ) );
    }

    protected <RelatedType, RelatedBean> void addRelationship( String tag, Consumer<RelatedType> setter,
                                                               Function<RelatedBean, RelatedType> adapter,
                                                               boolean optional, boolean uniqe,
                                                               Supplier<Entity<RelatedBean>> constructor )
    {
        elements.add( new Relationship<>( tag, setter, adapter, optional, uniqe, constructor ) );
    }

    protected void addCustomElement( Constituent<?, ?> element )
    {
        elements.add( element );
    }
}