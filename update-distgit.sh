#!/bin/sh
name=javapackages-bootstrap
. ./libbleed.sh

#rm -rf archive/
#./downstream.sh archive
cp $out/$name-$suffix.tar.zst archive/
cp archive/* $out/

./downstream.sh source-list | while read s; do
    sed -i '/^Source:.*\[\.\.\.\]/i'"$s" $out/$name.spec
done
sed -i '/^Source:.*\[\.\.\.\]/d' $out/$name.spec

./mbi.sh licensing >$out/$name-PACKAGE-LICENSING
sed -i "s/^License:.*/$(sed -n /^License:/p $out/$name-PACKAGE-LICENSING)/" $out/$name.spec

./downstream.sh source-manifest >>$out/sources
