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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SPDX expression parser.
 * 
 * @author Mikolaj Izdebski
 */
class SPDX
{
    private static final Pattern ID_REGEX = Pattern.compile( "([a-zA-Z0-9.-]+).*" );

    private final String rawExpression;

    private int parserPos;

    private final String niceExpression;

    public SPDX( String rawExpression )
    {
        this.rawExpression = rawExpression;
        niceExpression = parseExpression().toString();

    }

    private void parseError( String msg )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "Syntax error - " + msg + "\n" );
        sb.append( "  SPDX expression: \"" + rawExpression + "\"\n" );
        sb.append( "  Location: here ---" + new String( "-" ).repeat( parserPos ) + "^" );
        throw new RuntimeException( sb.toString() );
    }

    private void parseEof()
    {
        if ( parserPos != rawExpression.length() )
        {
            parseError( "Expected EOF" );
        }
    }

    private boolean matches( String op )
    {
        if ( rawExpression.substring( parserPos ).startsWith( op ) )
        {
            parserPos += op.length();
            return true;
        }
        return false;
    }

    private AST parseAtom()
    {
        Matcher matcher = ID_REGEX.matcher( rawExpression.substring( parserPos ) );
        if ( !matcher.matches() )
        {
            parseError( "Expected identifier" );
        }
        String tag = matcher.group( 1 );
        parserPos += tag.length();
        return new Atom( tag );
    }

    private AST parsePrimary()
    {
        if ( matches( "(" ) )
        {
            AST inner = parseUnion();
            if ( !matches( ")" ) )
            {
                parseError( "Unclosed parenthesis" );
            }
            return inner;
        }
        AST lhs = parseAtom();
        if ( matches( " WITH " ) )
        {
            AST rhs = parseAtom();
            return new Operator( " WITH ", 1, lhs, rhs );
        }
        return lhs;
    }

    private AST parseIntersection()
    {
        AST lhs = parsePrimary();
        if ( matches( " AND " ) )
        {
            AST rhs = parseIntersection();
            return new Operator( " AND ", 2, lhs, rhs );
        }
        return lhs;
    }

    private AST parseUnion()
    {
        AST lhs = parseIntersection();
        if ( matches( " OR " ) )
        {
            AST rhs = parseUnion();
            return new Operator( " OR ", 3, lhs, rhs );
        }
        return lhs;
    }

    private AST parseExpression()
    {
        AST expr = parseUnion();
        parseEof();
        return expr;
    }

    @Override
    public String toString()
    {
        return niceExpression;
    }
}