set -eu

ref=HEAD
if [ $# -gt 0 ]; then
    ref="$1"
    shift
fi

commit=$(git rev-parse $ref)
version=$(git describe --tags $commit)
rpmversion="$version"
suffix="$version"

case "$version" in
    *-*)
        version=$(git describe --tags --abbrev=0 $commit)
        timestamp=$(git cat-file commit $commit | sed -n '/^author /s/.* \([0-9][0-9]*\) [+-][0-9][0-9]*$/\1/;T;p;q')
        time=$(TZ=UTC date -d @$timestamp '+%Y%m%d.%H%M%S')
        shortcommit=$(echo "$commit" | sed 's/\(.......\).*/\1/')
        rpmversion="$version^$time.git.$shortcommit"
        suffix="snapshot-$time-$shortcommit"
        ;;
esac

out=rpm
if [[ $# -gt 0 ]]; then
    out="$1"
    shift
else
    mkdir -p $out/
fi

git archive $commit --prefix $name-$suffix/ | zstd -12 >$out/$name-$suffix.tar.zst
(cd ./$out && sha512sum --tag $name-$suffix.tar.zst) >$out/sources

cp $name.spec $out/$name.spec
sed -i "s/^Version:.*/Version:        $rpmversion/" $out/$name.spec

if [ "$suffix" != "$version" ]; then
    sed -i "/^Source:.*https:/s/^Source:.*/Source:         $name-$suffix.tar.zst/" $out/$name.spec
fi
