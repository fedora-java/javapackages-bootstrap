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
 * Indicates an exception that occurred during XML serialization or deserialization, such as
 * malformed XML or XML not conforming to the expected schema.
 *
 * @author Mikolaj Izdebski
 */
public class XMLException extends Exception {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new XML exception with the specified detail message and no cause.
     *
     * @param message the exception detail message
     * @see Exception#Exception(String)
     */
    public XMLException(String message) {
        super(message);
    }

    /**
     * Constructs a new XML exception with the specified detail message and cause.
     *
     * @param message the exception detail message
     * @param cause the cause of this exception
     * @see Exception#Exception(String,Throwable)
     */
    public XMLException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new XML exception with the specified cause and no detail message.
     *
     * @param cause the cause of this exception
     * @see Exception#Exception(Throwable)
     */
    public XMLException(Throwable cause) {
        super(cause);
    }
}
