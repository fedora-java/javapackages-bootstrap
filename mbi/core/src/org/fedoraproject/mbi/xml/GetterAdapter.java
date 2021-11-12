/*-
 * Copyright (c) 2021 Red Hat, Inc.
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

import java.util.Collections;

/**
 * @author Mikolaj Izdebski
 */
public class GetterAdapter<OuterType, NestedType>
    implements Getter<OuterType, Iterable<NestedType>>
{
    private final Getter<OuterType, NestedType> delegate;

    public GetterAdapter( Getter<OuterType, NestedType> delegate )
    {
        this.delegate = delegate;
    }

    @Override
    public Iterable<NestedType> get( OuterType object )
    {
        return Collections.singleton( delegate.get( object ) );
    }
}
