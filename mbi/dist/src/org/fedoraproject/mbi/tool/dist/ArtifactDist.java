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
package org.fedoraproject.mbi.tool.dist;

import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.spi.ToolProvider;

import org.fedoraproject.mbi.Reactor;
import org.fedoraproject.mbi.dist.DistRequest;
import org.fedoraproject.mbi.model.ModuleDescriptor;
import org.fedoraproject.xmvn.config.Artifact;
import org.fedoraproject.xmvn.config.Configurator;
import org.fedoraproject.xmvn.config.PackagingRule;
import org.fedoraproject.xmvn.config.Repository;
import org.fedoraproject.xmvn.locator.ServiceLocator;
import org.fedoraproject.xmvn.locator.ServiceLocatorFactory;
import org.fedoraproject.xmvn.metadata.ArtifactMetadata;
import org.fedoraproject.xmvn.metadata.Dependency;
import org.fedoraproject.xmvn.metadata.PackageMetadata;
import org.fedoraproject.xmvn.resolver.Resolver;
import org.fedoraproject.xmvn.tools.install.InstallationRequest;
import org.fedoraproject.xmvn.tools.install.Installer;
import org.fedoraproject.xmvn.tools.install.impl.DefaultInstaller;

/**
 * @author Mikolaj Izdebski
 */
class UMod
{
    final ModuleDescriptor md;

    final List<UArt> artifacts = new ArrayList<>();

    public UMod( ModuleDescriptor md )
    {
        this.md = md;
    }
}

/**
 * @author Mikolaj Izdebski
 */
abstract class GA
{
    final String gid;

    final String aid;

    public GA( String gid, String aid )
    {
        this.gid = gid;
        this.aid = aid;
    }

    @Override
    public String toString()
    {
        return gid + ":" + aid;
    }
}

/**
 * @author Mikolaj Izdebski
 */
class UArt
    extends GA
{
    final List<UAlias> aliases = new ArrayList<>();

    final List<UDep> deps = new ArrayList<>();

    final Set<String> versions = new LinkedHashSet<>();

    public UArt( String gid, String aid )
    {
        super( gid, aid );
    }

    Iterable<String> getVersions()
    {
        return versions.isEmpty() ? List.of( "SYSTEM" ) : versions;
    }
}

/**
 * @author Mikolaj Izdebski
 */
class UAlias
    extends GA
{
    final String classifier;

    public UAlias( String gid, String aid, String classifier )
    {
        super( gid, aid );
        this.classifier = classifier;
    }
}

/**
 * @author Mikolaj Izdebski
 */
class UDep
    extends GA
{
    final String ver;

    public UDep( String gid, String aid, String ver )
    {
        super( gid, aid );
        this.ver = ver;
    }

    @Override
    public String toString()
    {
        return super.toString() + ":" + ver;
    }
}

/**
 * @author Mikolaj Izdebski
 */
class Director
{
    private final Reactor reactor;

    private final DistRequest dist;

    private final Path workDir;

    private final Configurator configurator;

    private final Installer installer;

    private final PackageMetadata packageMetadata = new PackageMetadata();

    public Director( DistRequest dist )
    {
        this.reactor = dist.getReactor();
        this.dist = dist;
        this.workDir = dist.getWorkDir();

        ServiceLocator locator = new ServiceLocatorFactory().createServiceLocator();
        configurator = locator.getService( Configurator.class );
        Resolver resolver = locator.getService( Resolver.class );
        installer = new DefaultInstaller( configurator, resolver );
    }

    public void deployPom( ModuleDescriptor module )
        throws Exception
    {
        Path pomPropsPath = reactor.getPomPropertiesPath( module );
        try ( Reader reader = Files.newBufferedReader( pomPropsPath, StandardCharsets.UTF_8 ) )
        {
            Properties pomProps = new Properties();
            pomProps.load( reader );
            String gid = pomProps.getProperty( "groupId" );
            String aid = pomProps.getProperty( "artifactId" );
            deploy( module, gid, aid, null );
        }
    }

    public void deployJar( ModuleDescriptor module, UArt art )
        throws Exception
    {
        deploy( module, art.gid, art.aid, art );
    }

    public void deploy( ModuleDescriptor module, String gid, String aid, UArt art )
        throws Exception
    {
        Path artifactsDir = workDir.resolve( "artifacts" );
        Files.createDirectories( artifactsDir );
        String version = reactor.getProject( module.getProjectName() ).getMBIVersion();

        ArtifactMetadata pom = new ArtifactMetadata();
        pom.setGroupId( gid );
        pom.setArtifactId( aid );
        pom.setExtension( "pom" );
        pom.setVersion( version );

        if ( art != null )
        {
            Path classes = reactor.getClassesDir( module );
            Path mf = classes.resolve( "META-INF" ).resolve( "MANIFEST.MF" );
            if ( !Files.isRegularFile( mf ) )
            {
                Files.createDirectories( mf.getParent() );
                try ( BufferedWriter bw = Files.newBufferedWriter( mf ) )
                {
                    bw.write( "Manifest-Version: 1.0\n" );
                }
            }
            Path jarPath = artifactsDir.resolve( aid + "-" + version + ".jar" );
            ToolProvider jarTool = ToolProvider.findFirst( "jar" ).get();
            System.err.println( "Creating jar file " + jarPath.getFileName() );
            int ret = jarTool.run( System.out, System.err, "cfm", jarPath.toString(), mf.toString(), "-C",
                                   classes.toString(), "." );
            if ( ret != 0 )
            {
                throw new Exception( "jar tool failed with exit code " + ret );
            }
            ArtifactMetadata jar = new ArtifactMetadata();
            jar.setGroupId( gid );
            jar.setArtifactId( aid );
            jar.setVersion( version );
            jar.setPath( jarPath.toString() );
            packageMetadata.addArtifact( jar );
            for ( var dep : art.deps )
            {
                Dependency dependency = new Dependency();
                dependency.setGroupId( dep.gid );
                dependency.setArtifactId( dep.aid );
                dependency.setRequestedVersion( dep.ver );
                pom.addDependency( dependency );
            }
        }
        else
        {
            pom.addProperty( "type", "pom" );
        }

        Path pomPath = reactor.getPomPath( module );
        if ( !Files.isRegularFile( pomPath ) )
        {
            Files.createDirectories( pomPath.getParent() );
            try ( Writer writer = Files.newBufferedWriter( pomPath, StandardCharsets.UTF_8 ) )
            {
                writer.write( "Dummy POM file for " + module.getName() + " module" );
            }
        }
        pom.setPath( pomPath.toString() );
        packageMetadata.addArtifact( pom );

        if ( art != null )
        {
            Artifact artifactGlob = new Artifact();
            artifactGlob.setGroupId( gid );
            artifactGlob.setArtifactId( aid );
            artifactGlob.setVersion( version );
            PackagingRule rule = new PackagingRule();
            rule.setArtifactGlob( artifactGlob );
            for ( var alias : art.aliases )
            {
                var aa = new Artifact();
                aa.setGroupId( alias.gid );
                aa.setArtifactId( alias.aid );
                aa.setClassifier( alias.classifier );
                rule.addAlias( aa );
            }
            rule.setVersions( new ArrayList<>( art.versions ) );
            configurator.getConfiguration().addArtifactManagement( rule );
        }
    }

    public void install()
        throws Exception
    {
        Path planPath = workDir.resolve( "plan.xml" );
        packageMetadata.writeToXML( planPath );

        Repository installRepo = new Repository();
        installRepo.setId( "javapackages-bootstrap-install" );
        installRepo.setType( "jpp" );
        installRepo.addProperty( "root", dist.getArtifactsPath().toString() );
        configurator.getConfiguration().addRepository( installRepo );
        configurator.getConfiguration().getInstallerSettings().setMetadataDir( dist.getMetadataPath().toString() );

        InstallationRequest request = new InstallationRequest();
        request.setBasePackageName( dist.getBasePackageName() );
        request.setInstallRoot( dist.getInstallRoot() );
        request.setInstallationPlan( planPath );
        request.setRepositoryId( installRepo.getId() );
        request.setDescriptorRoot( workDir );

        installer.install( request );
    }
}

/**
 * @author Mikolaj Izdebski
 */
public class ArtifactDist
{
    private final Reactor reactor;

    private final DistRequest dist;

    public ArtifactDist( DistRequest dist )
    {
        this.reactor = dist.getReactor();
        this.dist = dist;
    }

    private List<UMod> parseTextMetadata( Path source )
        throws Exception
    {
        List<UMod> mods = new ArrayList<>();
        UMod mod = null;
        UArt uart = null;
        for ( String lineStr : Files.readAllLines( source ) )
        {
            String[] line = lineStr.trim().split( " +" );
            switch ( line[0] )
            {
                case "MOD":
                    mod = new UMod( reactor.getModule( line[1] ) );
                    mods.add( mod );
                    break;
                case "ART":
                    uart = new UArt( line[1], line[2] );
                    mod.artifacts.add( uart );
                    break;
                case "CVER":
                    uart.versions.add( line[1] );
                    break;
                case "ALIAS":
                    uart.aliases.add( new UAlias( line[1], line[2], line.length > 3 ? line[3] : "" ) );
                    break;
                case "DEP":
                    uart.deps.add( new UDep( line[1], line[2], line.length > 3 ? line[3] : "SYSTEM" ) );
                    break;
                default:
                    throw new IllegalStateException( "Unknown token: " + line[0] );
            }
        }
        return mods;
    }

    private void resolveDeps( List<UMod> mods )
    {
        Map<String, UMod> depmap = new LinkedHashMap<>();
        for ( var mod : mods )
        {
            for ( var art : mod.artifacts )
            {
                for ( var ver : art.getVersions() )
                {
                    depmap.put( art.toString() + ":" + ver, mod );
                }
                for ( var alias : art.aliases )
                {
                    for ( var ver : art.getVersions() )
                    {
                        depmap.put( alias.toString() + ":" + ver, mod );
                    }
                }
            }
        }
        for ( var mod : mods )
        {
            for ( var art : mod.artifacts )
            {
                for ( var dep : art.deps )
                {
                    UMod depMod = depmap.get( dep.toString() );
                    if ( depMod == null )
                    {
                        throw new Error( "Unsatisfied dep: " + art + ":" + art.getVersions().iterator().next() + " -> "
                            + dep );
                    }
                }
            }
        }
    }

    private void dumpUMD( List<UMod> mods )
        throws Exception
    {
        PrintWriter pw = new PrintWriter( "/tmp/dump.txt", StandardCharsets.UTF_8 );
        for ( var mod : mods )
        {
            pw.printf( "MOD %s%n", mod.md.getName() );
            for ( var umd : mod.artifacts )
            {
                pw.printf( "  ART %s %s%n", umd.gid, umd.aid );
                for ( String ver : umd.versions )
                {
                    pw.printf( "    CVER %s%n", ver );
                }
                for ( var ua : umd.aliases )
                {
                    pw.printf( "    ALIAS %s %s%s%n", ua.gid, ua.aid,
                               ua.classifier.isEmpty() ? "" : " " + ua.classifier );
                }
                for ( var ud : umd.deps )
                {
                    pw.printf( "    DEP %s %s%s%n", ud.gid, ud.aid, ud.ver.equals( "SYSTEM" ) ? "" : " " + ud.ver );
                }
            }
        }
        pw.close();
    }

    public Map<String, String> doDist()
        throws Exception
    {
        Path mdTxtPath = reactor.getRootDir().resolve( "mbi/dist/metadata.txt" );
        List<UMod> mods = parseTextMetadata( mdTxtPath );
        resolveDeps( mods );
        Collections.sort( mods, ( a, b ) -> a.md.getName().compareTo( b.md.getName() ) );
        dumpUMD( mods );

        Map<String, String> mod2art = new LinkedHashMap<>();
        Director director = new Director( dist );
        for ( var mod : mods )
        {
            for ( var art : mod.artifacts )
            {
                director.deployJar( mod.md, art );
                mod2art.put( mod.md.getName(),
                             art.aid + ( art.versions.isEmpty() ? "" : "-" + art.versions.iterator().next() ) );
            }
        }
        for ( var mod : reactor.getModules() )
        {
            if ( Files.isRegularFile( reactor.getPomPropertiesPath( mod ) ) )
            {
                director.deployPom( mod );
            }
        }
        director.install();
        return mod2art;
    }
}
