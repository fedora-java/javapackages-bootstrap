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


Copying
-------

Java Packages Bootstrap is free software. You can redistribute and/or
modify it under the terms specified in the LICENSE file.
This software comes with ABSOLUTELY NO WARRANTY.
