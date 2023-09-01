/*-
 * Copyright (c) 2023 Red Hat, Inc.
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
package org.fedoraproject.mbi.licensing;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * SPDX operator AST node.
 * 
 * @author Mikolaj Izdebski
 */
class Operator
    extends AST
{
    private final int precedence;

    private final String lexem;

    private Set<AST> children = new LinkedHashSet<>();

    public Operator( String lexem, int precedence, AST... args )
    {
        this.precedence = precedence;
        this.lexem = lexem;
        for ( AST arg : args )
        {
            if ( arg instanceof Operator && ( (Operator) arg ).precedence == precedence )
            {
                children.addAll( ( (Operator) arg ).children );
            }
            else
            {
                children.add( arg );
            }
        }
        if ( precedence > 1 )
        {
            children = new TreeSet<>( children );
        }
    }

    private String enclose( AST expr )
    {
        if ( expr instanceof Operator && ( (Operator) expr ).precedence > precedence )
        {
            return "(" + expr.toString() + ")";
        }
        return expr.toString();
    }

    @Override
    public String toString()
    {
        return children.stream().map( this::enclose ).collect( Collectors.joining( lexem ) );
    }
}