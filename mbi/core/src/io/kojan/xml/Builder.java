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
 * An object builder. Gathers information and captures it in its internal state, and then allows
 * building objects based on captured information.
 *
 * @param <Type> type of created objects
 * @author Mikolaj Izdebski
 */
public interface Builder<Type> {
    /**
     * Builds an object out of captured state.
     *
     * @return newly-built object
     */
    Type build();
}
