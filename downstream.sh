#!/bin/bash
# Copyright (c) 2020 Red Hat, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Written by Mikolaj Izdebski <mizdebsk@redhat.com>

set -eu

export LC_ALL='C.UTF-8'

git="git -c user.name=root -c user.email=root@localhost"

if [[ $# -eq 0 ]]; then
    echo "$0: command must be specified as first argument" >&2
    exit 1
fi
cmd="$1"
shift

if [[ $# -eq 0 ]]; then
    set project/*.properties
    set ${@/#project\/}
    set ${@/%.properties}
fi

function sub_version()
{
    eval local var=$1 val=\$$1
    set ${version/\~/ }
    case $val in *@@*) val=${val//@@/$2};; esac
    set ${1//\./ }
    set $@ $@
    while :; do
        case $val in *@*) val=${val/@/$1}; shift;; *) break;; esac
    done
    eval $var=$val
}

function clone()
{
    if [[ "$type" = git ]]; then
        if [[ -d "upstream/$p.git" ]]; then
            $git -C "upstream/$p.git" remote set-url origin "$url"
            $git -C "upstream/$p.git" fetch
        else
            $git clone --mirror "$url" "upstream/$p.git"
        fi
    elif [[ "$type" = zip ]]; then
        curl -f -o "upstream/$p-$version.zip" "$url"
    else
        echo "$0: $p: unsupported upstream type: $type" >&2
        exit 1
    fi
}

function prep()
{
    rm -rf "downstream/$p"
    if [[ "$type" = git ]]; then
        $git clone -n "upstream/$p.git" "downstream/$p"
        $git -C "downstream/$p" checkout -b upstream-base "$ref"
        commit=$($git -C "downstream/$p" rev-parse upstream-base)
        date=$($git -C "downstream/$p" cat-file commit $commit | sed -n '/^author /s/.* \([0-9][0-9]* [+-][0-9][0-9]*$\)/\1/;T;p;q')
    elif [[ "$type" = zip ]]; then
        date="100000000 +0000"
        $git init "downstream/$p"
        unzip "upstream/$p-$version.zip" -d "downstream/$p"
        dir="downstream/$p"/$(ls "downstream/$p")
        if [[ -d "$dir" ]]; then
            mv "$dir"/* "downstream/$p"
            rmdir "$dir"
        fi
        $git -C "downstream/$p" add .
        $git -C "downstream/$p" commit --date "$date" -m "Import version $version"
        $git -C "downstream/$p" checkout -b upstream-base
    else
        echo "$0: $p: unsupported upstream type: $type" >&2
        exit 1
    fi
    find "downstream/$p" -type f -regextype posix-egrep -regex '.*\.(class|jar|bar|war|ear|zip|gz|7z|bz2|ttf|so|dll|o|exe|dylib|jnilib)$' -delete
    if [[ "$p" == bnd ]]; then
        rm -r downstream/bnd/docs/
    elif [[ "$p" == easymock ]]; then
        rm -r downstream/easymock/website/js/vendor/
    elif [[ "$p" == qdox ]]; then
        rm -r downstream/qdox/bootstrap/
    elif [[ "$p" == mockito ]]; then
        rm -r downstream/mockito/src/javadoc/
    elif [[ "$p" == jansi ]]; then
        rm -r downstream/jansi/src/main/native/
    elif [[ "$p" == jflex ]]; then
        rm -r downstream/jflex/jflex/examples/
    fi
    $git -C "downstream/$p" commit --date "$date" --allow-empty -a -m 'Remove binary files'
    $git -C "downstream/$p" checkout -b downstream upstream-base
    if [[ -d patches/$p ]]; then
        set patches/$p/*.patch
        set ${@//patches/../../patches}
        $git -C "downstream/$p" am --ignore-whitespace $@
    fi
}

function archive()
{
    mkdir -p archive
    rm -f archive/$p.tar archive/$p.tar.zst
    commit=$($git -C "downstream/$p" rev-parse upstream-base)
    date=$($git -C "downstream/$p" cat-file commit $commit | sed -n '/^author /s/.* \([0-9][0-9]* [+-][0-9][0-9]*$\)/\1/;T;p;q')
    tree=$($git -C "downstream/$p" cat-file commit $commit | sed -n 's/^tree //;T;p;q')
    $git -C "downstream/$p" archive --prefix downstream/$p/ --mtime "$date" "$tree" >archive/$p.tar
    zstd --rm -T8 -12 archive/$p.tar
}

function bundled_provides()
{
    echo "bundled(${rpm_name:-$p}) = $version"
}

for p; do
    if [[ ! -f project/$p.properties ]]; then
        echo "$0: $p: upstream descriptor not found" >&2
        exit 1
    fi
    unset url
    unset ref
    unset version
    unset rpm_name
    type=git
    . project/$p.properties
    sub_version ref
    sub_version url
    if [[ "$cmd" = clone ]] || [[ "$cmd" = prep ]]; then
        echo >&2
        echo "=============================================" >&2
        echo "project: $p" >&2
        echo "version: $version" >&2
        echo "url:     $url" >&2
        echo "ref:     $ref" >&2
        echo "=============================================" >&2
    fi
    if [[ "$cmd" = clone ]]; then
        clone
    elif [[ "$cmd" = prep ]]; then
        prep
    elif [[ "$cmd" = archive ]]; then
        archive
    elif [[ "$cmd" = bundled-provides ]]; then
        bundled_provides
    else
        echo "$0: unknown command: $cmd" >&2
        exit 1
    fi
done
