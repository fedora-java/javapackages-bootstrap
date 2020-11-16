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
package org.fedoraproject.mbi;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.fedoraproject.mbi.model.ModuleDescriptor;
import org.fedoraproject.mbi.model.ProjectDescriptor;
import org.fedoraproject.mbi.model.io.ProjectDescriptorReader;

/**
 * @author Mikolaj Izdebski
 */
public class Reactor
{
    private final Path reactorRootDir;

    private final Map<String, ProjectDescriptor> projectsByName = new LinkedHashMap<>();

    private final Map<String, ModuleDescriptor> modulesByName = new LinkedHashMap<>();

    public Reactor( Path reactorRootDir )
        throws Exception
    {
        this.reactorRootDir = reactorRootDir;
        loadProjects();
    }

    public Path getRootDir()
    {
        return reactorRootDir;
    }

    public ProjectDescriptor getProject( String name )
    {
        return projectsByName.get( name );
    }

    public ModuleDescriptor tryGetModule( String name )
    {
        return modulesByName.get( name );
    }

    public ModuleDescriptor getModule( String name )
    {
        ModuleDescriptor module = tryGetModule( name );
        if ( module == null )
        {
            throw new NoSuchElementException( "No such module: " + name );
        }
        return module;
    }

    public Collection<ProjectDescriptor> getProjects()
    {
        return projectsByName.values();
    }

    public Collection<ModuleDescriptor> getModules()
    {
        return modulesByName.values();
    }

    public Collection<ProjectDescriptor> getProjects( Collection<String> filter )
    {
        if ( filter.isEmpty() )
        {
            return getProjects();
        }
        Collection<ProjectDescriptor> projects = new LinkedHashSet<>();
        for ( String project : filter )
        {
            projects.add( getProject( project ) );
        }
        return projects;
    }

    public Collection<ModuleDescriptor> getModules( Collection<String> filter )
    {
        if ( filter.isEmpty() )
        {
            return getModules();
        }
        Collection<ModuleDescriptor> modules = new LinkedHashSet<>();
        for ( String module : filter )
        {
            modules.add( getModule( module ) );
        }
        return modules;
    }

    private void addProject( ProjectDescriptor project )
    {
        projectsByName.put( project.getName(), project );
        for ( ModuleDescriptor module : project.getModules() )
        {
            modulesByName.put( module.getName(), module );
        }
    }

    private Properties loadProperties( Path directory, String name )
        throws IOException
    {
        Properties props = new Properties();
        Path path = directory.resolve( name + ".properties" );
        if ( Files.exists( path ) )
        {
            try ( Reader reader = Files.newBufferedReader( path ) )
            {
                props.load( reader );
            }
        }
        return props;
    }

    private void loadProjects()
        throws Exception
    {
        var projectDescriptorReader = new ProjectDescriptorReader();
        try ( DirectoryStream<Path> ds = Files.newDirectoryStream( getRootDir().resolve( "project" ), "*.xml" ) )
        {
            for ( Path path : ds )
            {
                String name = path.getFileName().toString().replaceAll( "\\.xml$", "" );
                Properties properties = loadProperties( path.getParent(), name );
                try ( Reader reader = Files.newBufferedReader( path ) )
                {
                    addProject( projectDescriptorReader.read( name, properties, reader ) );
                }
            }
        }
    }

    public static Reactor defaultReactor()
        throws Exception
    {
        return new Reactor( Paths.get( "." ).toRealPath() );
    }

    public Path getProjectDir( ProjectDescriptor project )
    {
        if ( project.getName().equals( "mbi" ) )
        {
            return reactorRootDir.resolve( "mbi" );
        }
        return reactorRootDir.resolve( "downstream" ).resolve( project.getName() );
    }

    public Path getSourceRootDir( ModuleDescriptor module )
    {
        Path projectDir = getProjectDir( getProject( module.getProjectName() ) );
        return module.getProjectSubDir() != null ? projectDir.resolve( module.getProjectSubDir() ) : projectDir;
    }

    public Path getTargetDir( ModuleDescriptor module )
    {
        return getRootDir().resolve( "build" ).resolve( module.getName() );
    }

    public Path getGeneratedSourcesDir( ModuleDescriptor module )
    {
        return getTargetDir( module ).resolve( "generated-sources" );
    }

    public Path getClassesDir( ModuleDescriptor module )
    {
        return getTargetDir( module ).resolve( "classes" );
    }

    public Path getPomPath( ModuleDescriptor module )
    {
        return getTargetDir( module ).resolve( "pom.xml" );
    }

    public Path getPomPropertiesPath( ModuleDescriptor module )
    {
        return getTargetDir( module ).resolve( "pom.properties" );
    }

    public List<Path> getClassPath( ModuleDescriptor module )
    {
        ArrayList<Path> classPath = new ArrayList<>();
        for ( String depName : module.getDependencies() )
        {
            ModuleDescriptor dep = getModule( depName );
            classPath.add( getClassesDir( dep ) );
        }
        return classPath;
    }
}
