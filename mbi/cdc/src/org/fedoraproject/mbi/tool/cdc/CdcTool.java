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
package org.fedoraproject.mbi.tool.cdc;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.fedoraproject.mbi.tool.Instruction;
import org.fedoraproject.mbi.tool.Tool;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Mikolaj Izdebski
 */
public class CdcTool
    extends Tool
{
    private static final String PLEXUS_DESCRIPTOR_PATH = "META-INF/plexus/components.xml";

    private static final String SISU_DESCRIPTOR_PATH = "META-INF/sisu/javax.inject.Named";

    private DocumentBuilder documentBuilder;

    private Transformer transformer;

    private Document plexusDescriptor;

    private Element plexusComponents;

    private List<String> sisuComponents = new ArrayList<>();

    @Override
    public void initialize()
        throws Exception
    {
        documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty( OutputKeys.INDENT, "yes" );
        transformer.setOutputProperty( "{http://xml.apache.org/xslt}indent-amount", "2" );
        plexusDescriptor = documentBuilder.newDocument();
        plexusDescriptor.setXmlStandalone( true );
        Element componentSet = plexusDescriptor.createElement( "component-set" );
        plexusDescriptor.appendChild( componentSet );
        plexusComponents = addChild( componentSet, "components" );
    }

    public Document getDescriptor()
    {
        return plexusDescriptor;
    }

    private Element addChild( Element parent, String name )
    {
        Element child = plexusDescriptor.createElement( name );
        parent.appendChild( child );
        return child;
    }

    private void addTextField( Element parent, String name, Object value )
    {
        Element child = plexusDescriptor.createElement( name );
        child.setTextContent( value.toString() );
        parent.appendChild( child );
    }

    public Element addComponent( String impl, String role, String hint )
    {
        Element comp = addChild( plexusComponents, "component" );
        addTextField( comp, "implementation", impl );
        addTextField( comp, "role", role );
        addTextField( comp, "role-hint", hint );
        return addChild( comp, "requirements" );
    }

    @Instruction
    public void addComponent( String comp )
    {
        String[] elements = comp.split( "\\|" );
        addComponent( elements[0], elements[1], elements[2] );
    }

    private void addRequirement( Element requirements, String field, String role, String hint, boolean optional )
    {
        Element req = addChild( requirements, "requirement" );
        addTextField( req, "field-name", field );
        addTextField( req, "role", role );
        addTextField( req, "role-hint", hint );
        addTextField( req, "optional", optional );
    }

    @Instruction
    public void gleanFromXML( String xmlPath )
        throws Exception
    {
        Path path = getSourceRootDir().resolve( xmlPath );
        Document partialDescriptor = documentBuilder.parse( path.toFile() );
        NodeList partialComponents = partialDescriptor.getElementsByTagName( "component" );
        for ( int i = 0; i < partialComponents.getLength(); i++ )
        {
            Node component = partialComponents.item( i );
            plexusComponents.appendChild( plexusDescriptor.importNode( component, true ) );
        }
    }

    private void gleanFromClasses()
        throws Exception
    {
        for ( Path classFile : Files.walk( getClassesDir() ).filter( Files::isRegularFile ).filter( path -> !path.startsWith( getClassesDir().resolve( "META-INF" ) ) && path.toString().endsWith( ".class" ) ).collect( Collectors.toList() ) )
        {
            String className =
                getClassesDir().relativize( classFile ).toString().replaceAll( ".class$", "" ).replace( '/', '.' );
            Class<?> cls = getClass().getClassLoader().loadClass( className );
            if ( cls.isAnnotationPresent( Named.class ) )
            {
                sisuComponents.add( cls.getName() );
            }
            if ( cls.isAnnotationPresent( Component.class ) )
            {
                Annotation plexus = cls.getAnnotationsByType( Component.class )[0];
                Class<?> role = (Class<?>) Component.class.getMethod( "role" ).invoke( plexus );
                String hint = (String) Component.class.getMethod( "hint" ).invoke( plexus );
                Element requirements = addComponent( cls.getName(), role.getName(), hint );
                for ( ; cls != null; cls = cls.getSuperclass() )
                {
                    for ( Field f : cls.getDeclaredFields() )
                    {
                        if ( f.isAnnotationPresent( Requirement.class ) )
                        {
                            Annotation req = f.getAnnotationsByType( Requirement.class )[0];
                            Class<?> reqRole = (Class<?>) Requirement.class.getMethod( "role" ).invoke( req );
                            String reqHint = (String) Requirement.class.getMethod( "hint" ).invoke( req );
                            boolean optional = (boolean) Requirement.class.getMethod( "optional" ).invoke( req );
                            if ( reqRole.isAssignableFrom( Object.class ) )
                            {
                                reqRole = f.getType();
                            }
                            addRequirement( requirements, f.getName(), reqRole.getName(), reqHint, optional );
                        }
                    }
                }
            }
        }
    }

    @Override
    public void execute()
        throws Exception
    {
        gleanFromClasses();

        if ( sisuComponents.isEmpty() && !plexusComponents.hasChildNodes() )
        {
            throw new RuntimeException( "No Plexus components were discovered by CDC for module "
                + getModule().getName() );
        }

        Path plexusPath = getClassesDir().resolve( PLEXUS_DESCRIPTOR_PATH );
        Files.createDirectories( plexusPath.getParent() );
        Source source = new DOMSource( plexusDescriptor );
        Result result = new StreamResult( plexusPath.toFile() );
        transformer.transform( source, result );

        Path sisuPath = getClassesDir().resolve( SISU_DESCRIPTOR_PATH );
        Files.createDirectories( sisuPath.getParent() );
        try ( PrintWriter pw = new PrintWriter( Files.newBufferedWriter( sisuPath ) ) )
        {
            for ( String sisuComponent : sisuComponents )
            {
                pw.println( sisuComponent );
            }
        }
    }
}
