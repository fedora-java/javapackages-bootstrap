#!/bin/sh
set -eu

if [[ $# -lt 1 ]]; then
    echo error: Version must be speficied on command-line >&2
    exit 1
fi
version="$1"

out=rpm
if [[ $# -gt 1 ]]; then
    out="$2"
else
    mkdir -p rpm/
fi

rm -rf archive/
./downstream.sh archive

(
    sed -n '/^Source:.*tar.zst$/q;p' javapackages-bootstrap.spec
    ./downstream.sh source-list
    sed -n '/^Source:.*tar.zst$/{:x;n;/^Source:.*tar.zst$/bx;:y;p;n;by;}' javapackages-bootstrap.spec
) >$out/javapackages-bootstrap.spec

./mbi.sh licensing >$out/javapackages-bootstrap-PACKAGE-LICENSING
sed -i "s/^License:.*/$(sed -n /^License:/p $out/javapackages-bootstrap-PACKAGE-LICENSING)/" $out/javapackages-bootstrap.spec

git archive HEAD --prefix javapackages-bootstrap-$version/ | zstd -12 >archive/javapackages-bootstrap-$version.tar.zst
cp archive/* $out/

(cd ./archive && sha512sum --tag javapackages-bootstrap-$version.tar.zst) >$out/sources
./downstream.sh source-manifest >>$out/sources

sed -i "s/^Version:.*/Version:        $version/" $out/javapackages-bootstrap.spec
