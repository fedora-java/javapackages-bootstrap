#!/usr/lib/jvm/java-11/bin/java --source 8
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

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import javax.tools.*;

/**
 * @author Mikolaj Izdebski
 */
public class Stage1 {
    public static void main(String[] args) throws Exception {
        Files.createDirectories(Paths.get("build/mbi-launcher/classes"));
        System.exit(ToolProvider.getSystemJavaCompiler()
                    .getTask(null, null, null, Arrays.asList("-d", "build/mbi-launcher/classes"), null,
                             ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null)
                             .getJavaFileObjectsFromFiles(Files.walk(Paths.get("mbi/core"))
                                                          .filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".java"))
                                                          .map(Path::toFile).collect(Collectors.toList())))
                    .call()
                    && new URLClassLoader(new URL[] { new File("build/mbi-launcher/classes").toURI().toURL() }).loadClass("org.fedoraproject.mbi.Main")
                    .getMethod("main", String[].class).invoke(null, (Object) args) == null ? 0 : 1);
    }
}
