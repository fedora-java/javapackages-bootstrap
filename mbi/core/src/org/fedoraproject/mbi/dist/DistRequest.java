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
package org.fedoraproject.mbi.dist;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.fedoraproject.mbi.Reactor;

/**
 * @author Mikolaj Izdebski
 */
public class DistRequest
{
    private final Reactor reactor;

    private final Path workDir;

    private final Path javaCmdPath;

    private final String basePackageName;

    private final Path installRoot;

    private final Path mavenHomePath;

    private final Path metadataPath;

    private final Path artifactsPath;

    private final Path launchersPath;

    private final Path licensesPath;

    private static Path relativePath( String pathStr )
    {
        Path absolutePath = Paths.get( pathStr );
        return absolutePath.getRoot().relativize( absolutePath );
    }

    public DistRequest( Reactor reactor, Path workDir, //
                        String javaCmdPath, String basePackageName, String installRoot, String mavenHomePath,
                        String metadataPath, String artifactsPath, String launchersPath, String licensesPath )
    {
        this.reactor = reactor;
        this.workDir = workDir;
        this.javaCmdPath = Paths.get( javaCmdPath );
        this.basePackageName = basePackageName;
        this.installRoot = Paths.get( installRoot );
        this.mavenHomePath = relativePath( mavenHomePath );
        this.metadataPath = relativePath( metadataPath );
        this.artifactsPath = relativePath( artifactsPath );
        this.launchersPath = relativePath( launchersPath );
        this.licensesPath = relativePath( licensesPath );
    }

    public Reactor getReactor()
    {
        return reactor;
    }

    public Path getWorkDir()
    {
        return workDir;
    }

    public Path getJavaCmdPath()
    {
        return javaCmdPath;
    }

    public String getBasePackageName()
    {
        return basePackageName;
    }

    public Path getInstallRoot()
    {
        return installRoot;
    }

    public Path getMavenHomePath()
    {
        return mavenHomePath;
    }

    public Path getMetadataPath()
    {
        return metadataPath;
    }

    public Path getArtifactsPath()
    {
        return artifactsPath;
    }

    public Path getLaunchersPath()
    {
        return launchersPath;
    }

    public Path getLicensesPath()
    {
        return licensesPath;
    }
}
