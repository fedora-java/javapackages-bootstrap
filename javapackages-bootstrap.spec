# Exclude automatically generated requires on java interpreter which is not
# owned by any package
%global __requires_exclude ^%{_jvmdir}/jre
# Generated list of bundled packages
%global _local_file_attrs local_generator
%global __local_generator_provides cat %{_builddir}/%{buildsubdir}/bundled-provides.txt
%global __local_generator_path ^%{metadataPath}/.*$
%global debug_package %{nil}
%global javaHomePath %{_jvmdir}/jre-21-openjdk
%global mavenHomePath %{_datadir}/%{name}
%global metadataPath %{mavenHomePath}/maven-metadata
%global artifactsPath %{_prefix}/lib
%global launchersPath %{_libexecdir}/%{name}

Name:           javapackages-bootstrap
Version:        [...]
Release:        %autorelease
Summary:        A means of bootstrapping Java Packages Tools
# For detailed info see the file javapackages-bootstrap-PACKAGE-LICENSING
License:        [...]
URL:            https://github.com/fedora-java/javapackages-bootstrap
ExclusiveArch:  %{java_arches}

Source:         https://github.com/fedora-java/javapackages-bootstrap/releases/download/%{version}/javapackages-bootstrap-%{version}.tar.zst
# License breakdown
Source:         javapackages-bootstrap-PACKAGE-LICENSING
# To obtain the following sources:
# tar -xf ${name}-${version}.tar.zst
# pushd ${name}-${version}
# ./downstream.sh clone
# ./downstream.sh prep
# ./downstream.sh archive
# The results are in the archive directory
Source:         [...]

BuildRequires:  byaccj
BuildRequires:  gcc
BuildRequires:  java-21-openjdk-devel
BuildRequires:  jurand
BuildRequires:  rpm-devel
Requires:       bash
Requires:       coreutils
Requires:       java-21-openjdk-devel
Requires:       javapackages-common
Requires:       lujavrite%{?_isa}
Requires:       procps-ng

%description
In a nutshell, Java Packages Bootstrap (JPB) is a standalone build of all Java
software packages that are required for Java Packages Tools (JPT) to work.

In order to achieve reliable and reproducible builds of Java packages while
meeting Fedora policy that requires everything to be built from source, without
using prebuilt binary artifacts, it is necessary to build the packages in a
well-defined, acyclic order. Dependency cycles between packages are the biggest
obstacle to achieving this goal and JPT is the biggest offender -- it requires
more than a hundred of Java packages, all of which in turn build-require JPT.

JPB comes with a solution to this problem -- it builds everything that JPT needs
to work, without reliance on any Java software other than OpenJDK. JPT can
depend on JPB for everything, without depending on any other Java packages. For
example, JPB contains embedded version of XMvn, removing dependency of JPT on
XMvn, allowing JPT to be used before one builds XMvn package.

%prep
%autosetup -p1 -C
mkdir archive/
cp %{sources} archive/
./downstream.sh prep-from-archive

%build
JAVA_HOME=%{javaHomePath} ./mbi.sh build -parallel

%install
JAVA_HOME=%{javaHomePath} ./mbi.sh dist \
  -javaCmdPath=%{javaHomePath}/bin/java \
  -basePackageName=%{name} \
  -installRoot=%{buildroot} \
  -mavenHomePath=%{mavenHomePath} \
  -metadataPath=%{metadataPath} \
  -artifactsPath=%{artifactsPath} \
  -launchersPath=%{launchersPath} \
  -licensesPath=%{_licensedir}/%{name} \

install -D -p -m 644 downstream/xmvn-generator/src/main/lua/xmvn-generator.lua %{buildroot}%{_rpmluadir}/%{name}-generator.lua
install -D -p -m 644 downstream/xmvn-generator/src/main/rpm/macros.xmvngen %{buildroot}%{_rpmmacrodir}/macros.jpbgen
install -D -p -m 644 downstream/xmvn-generator/src/main/rpm/macros.xmvngenhook %{buildroot}%{_sysconfdir}/rpm/macros.jpbgenhook
install -D -p -m 644 downstream/xmvn-generator/src/main/rpm/xmvngen.attr %{buildroot}%{_fileattrsdir}/jpbgen.attr

echo '
%%__xmvngen_debug 1
%%__xmvngen_libjvm %{javaHomePath}/lib/server/libjvm.so
%%__xmvngen_classpath %{artifactsPath}/%{name}/xmvn-generator.jar:%{artifactsPath}/%{name}/asm.jar:%{artifactsPath}/%{name}/commons-compress.jar:%{artifactsPath}/%{name}/commons-io.jar:%{artifactsPath}/%{name}/xmvn-mojo.jar:%{artifactsPath}/%{name}/kojan-xml.jar:%{artifactsPath}/%{name}/maven-model.jar:%{artifactsPath}/%{name}/plexus-utils.jar
%%__xmvngen_provides_generators org.fedoraproject.xmvn.generator.filesystem.FilesystemGeneratorFactory org.fedoraproject.xmvn.generator.jpscript.JPackageScriptGeneratorFactory org.fedoraproject.xmvn.generator.jpms.JPMSGeneratorFactory org.fedoraproject.xmvn.generator.maven.MavenGeneratorFactory
%%__xmvngen_requires_generators org.fedoraproject.xmvn.generator.filesystem.FilesystemGeneratorFactory org.fedoraproject.xmvn.generator.jpscript.JPackageScriptGeneratorFactory org.fedoraproject.xmvn.generator.maven.MavenGeneratorFactory
%%__xmvngen_post_install_hooks org.fedoraproject.xmvn.generator.transformer.TransformerHookFactory
%%jpb_env PATH=/usr/libexec/javapackages-bootstrap:$PATH
%%java_home %{javaHomePath}
' >%{buildroot}%{_rpmmacrodir}/macros.jpbgen

sed -i s/xmvn-generator/%{name}-generator/ %{buildroot}%{_sysconfdir}/rpm/macros.jpbgenhook
sed -i s/xmvn-generator/%{name}-generator/ %{buildroot}%{_fileattrsdir}/jpbgen.attr
sed -i s/_xmvngen_/_jpbgen_/ %{buildroot}%{_fileattrsdir}/jpbgen.attr

# Dynamically generate bundled Provides
./downstream.sh bundled-provides >bundled-provides.txt

%check
%{buildroot}%{launchersPath}/xmvn --version

%files
%{mavenHomePath}
%{metadataPath}/*
%{artifactsPath}/*
%{launchersPath}/*
%{_rpmluadir}/*
%{_rpmmacrodir}/*
%{_fileattrsdir}/*
%{_sysconfdir}/rpm/*

%license %{_licensedir}/%{name}
%doc README.md
%doc AUTHORS

%changelog
%autochangelog
