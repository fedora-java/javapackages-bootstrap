/*-
 * Copyright (c) 2024 Red Hat, Inc.
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
 * Converter function. Converts objects of one type into objects of another type.
 *
 * @param <SourceType> type of objects to convert
 * @param <TargetType> type of objects after conversion
 * @author Mikolaj Izdebski
 */
@FunctionalInterface
public interface Converter<SourceType, TargetType> {
    /**
     * Converts objects of one type into objects of another type.
     *
     * @param object to convert
     * @return converted object
     */
    TargetType convert(SourceType object);

    /**
     * No operation converter that converts object to itself.
     *
     * @param <Type> type of objects
     * @param object object to convert
     * @return the same object
     */
    static <Type> Type nop(Type object) {
        return object;
    }
}
