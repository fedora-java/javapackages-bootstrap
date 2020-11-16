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
package org.fedoraproject.mbi.model;

import java.util.Collections;
import java.util.List;

/**
 * @author Mikolaj Izdebski
 */
public class Execution
{
    private final String toolName;

    private final List<Instruction> instructions;

    public Execution( String toolName, List<Instruction> instructions )
    {
        this.toolName = toolName;
        this.instructions = Collections.unmodifiableList( instructions );
    }

    public String getToolName()
    {
        return toolName;
    }

    public List<Instruction> getInstructions()
    {
        return instructions;
    }
}
