Java Packages Bootstrap
=======================

A means of bootstrapping Java Packages Tools.


About
-----

In a nutshell, Java Packages Bootstrap (JPB) is a standalone build of
all Java software packages that are required for Java Packages Tools
(JPT) to work.

JPT provides packagers with numerous RPM macros and tools to make Java
packaging simpler and easier.  While it implements some of its simpler
functionality in shell and Python, it relies on external Java packages
for most of heavy lifting work.  Notably, %mvn_build and %mvn_install
RPM macros, which are the essence of JPT, are just thin wrappers for
XMvn, which is written in Java.

In order to achieve reliable and reproducible builds of Java packages
while meeting Fedora policy that requires everything to be built from
source, without using prebuilt binary artifacts, it is necessary to
build the packages in well-defined, acyclic order.  Dependency cycles
between packages are the biggest obstacle to achieving this goal and
JPT is the biggest offender -- it requires more then a hundred of Java
packages, all of which in turn build-require JPT.

JPB comes with a solution to this problem -- it builds everything hat
JPT needs to work, without reliance on any Java software other than
OpenJDK.  JPT can depend on JPB for everything, without depending on
any other Java packages.  For example, JPB contains embedded version
of XMvn, removing dependency of JPT on XMvn, allowing JPT to be used
before one builds XMvn package.


Directory structure
-------------------

* `mbi` - code of MBI, custom build system for javapackages-bootstrap.

* `project` - project descriptors, `*.properties` are upstream
  descriptors, `*.xml` are build descriptors.

* `upstream` - cache of pristine upstream sources (git repos or
  zipballs).

* `patches` - downstream patches in git format.

* `downstream` - upstream sources with problematic files removed and
  downstream patches applied.

* `build` - results of the build.

* `eclipse` - holds generated Eclipse projects that can be imported
  into Eclipse IDE.


MBI build system
----------------

Most of Java build systems are complex software projects with numerous
external dependencies.  For the purpose of bootstrapping Java packages
a minimalistic build system was created.

The core part of MBI has no external dependencies.  Additional
extension tools can have dependencies, but MBI is responsible for
building them from sources before particular tool can be used.

MBI has two basic concepts, project and module.  Project corresponds
to a single upstream project -- typically a single git repository.
Module corresponds to a single binary artifact.  More than one module
can be built from sources of the same project.


Project descriptors
-------------------

For each project there is a properties file in `project` directory
that links that project with corresponding upstream project.  That
file is called project descriptor, or upstream descriptor.

Project descriptor has the following properties defined:

* `version` - version of upstream project.

* `type` - determines the form in which upstream sources are fetched,
  can be either `git` (default; sources are obtained by cloning a Git
  repository) or `zip` (sources are downloaded as a ZIP file).

* `url` - URL of Git repo or ZIP file to get the sources from.

* `ref` - Git ref (commit ID, tag or branch) within Git repo to get
  sources from.


Module descriptors
------------------

Each module is described by an XML file in `project` directory that
specifies dependencies between modules and describes how to build
particular module.

For the sake of simplicity MBI doesn't distinguish between different
dependency scopes (such as runtime vs build dependencies) and doesn't
support transitive dependencies - all dependencies must be listed
explicitly.

Each module build is split into build steps.  Each build step involves
a single invocation of MBI tool on the module.  One tool can be
invoked more than once on the same module, in different build steps.

MBI provides the following tools that can be used as build steps:

* `compiler` - invokes Java compiler to compile Java source files into
  Java bytecode.

* `modello` - invokes Modello tool to generate code from data models.

* `munge` - invokes Munge, a simple Java preprocessor.

* `cdc` - component descriptor creator, extracts Plexus and Sisu
  component descriptors from Javadoc comments or Java annotations,
  enabling Sisu and Plexus to discover and load components implemented
  by the module.

* `pdc` - plugin descriptor creator, invokes Maven Plugin Tools to
  generate plugin descriptors, allowing module to be used as a Maven
  plugin.

* `pom` - transforms Maven POM and includes it as output artifact.

* `ant` - executes arbitrary Ant tasks.


Copying
-------

Java Packages Bootstrap is free software. You can redistribute and/or
modify it under the terms specified in the LICENSE file.
This software comes with ABSOLUTELY NO WARRANTY.
