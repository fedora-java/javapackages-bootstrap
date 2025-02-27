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
package io.kojan.xml;

/**
 * Bean property getter.
 *
 * @param <OuterType> bean class
 * @param <NestedType> type of property value
 * @author Mikolaj Izdebski
 */
@FunctionalInterface
public interface Getter<OuterType, NestedType> {
    /**
     * Returns the value of bean property.
     *
     * @param object the bean whose property should be returned
     * @return bean property value
     */
    NestedType get(OuterType object);
}
