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
        $git clone "upstream/$p.git" "downstream/$p"
        $git -C "downstream/$p" checkout -b upstream-base "$ref"
    elif [[ "$type" = zip ]]; then
        $git init "downstream/$p"
        unzip "upstream/$p-$version.zip" -d "downstream/$p"
        dir="downstream/$p"/$(ls "downstream/$p")
        if [[ -d "$dir" ]]; then
            mv "$dir"/* "downstream/$p"
            rmdir "$dir"
        fi
        $git -C "downstream/$p" add .
        $git -C "downstream/$p" commit -m "Import version $version"
        $git -C "downstream/$p" checkout -b upstream-base
    else
        echo "$0: $p: unsupported upstream type: $type" >&2
        exit 1
    fi
    find "downstream/$p" -type f -regextype posix-egrep -regex '.*\.(class|jar|bar|war|ear|zip|gz|7z|bz2|ttf|so|dll|o|exe|dylib|jnilib)$' -delete
    if [[ "$p" == bnd ]]; then
        rm -rf downstream/bnd/docs/
    elif [[ "$p" == easymock ]]; then
        rm -rf downstream/easymock/website/js/vendor/
    elif [[ "$p" == qdox ]]; then
        rm -rf downstream/qdox/bootstrap/
    elif [[ "$p" == mockito ]]; then
        rm -rf downstream/mockito/src/javadoc/
    elif [[ "$p" == jflex ]]; then
        rm -rf downstream/jflex/jflex/examples/
    elif [[ "$p" == jsoup ]]; then
        rm -rf downstream/jsoup/src/test/resources/
    elif [[ "$p" == testng ]]; then
        rm -rf downstream/testng/src/test/
        >downstream/testng/src/main/resources/org/testng/jquery-*.min.js
    fi
    $git -C "downstream/$p" commit --allow-empty -a -m 'Remove binary files'
    $git -C "downstream/$p" checkout -b downstream upstream-base
    if [[ -d patches/$p ]]; then
        set patches/$p/*.patch
        set ${@//patches/../../patches}
        $git -C "downstream/$p" am --ignore-whitespace $@
    fi
}

for p; do
    if [[ ! -f project/$p.properties ]]; then
        echo "$0: $p: upstream descriptor not found" >&2
        exit 1
    fi
    unset url
    unset ref
    unset version
    type=git
    . project/$p.properties
    sub_version ref
    sub_version url
    echo
    echo =============================================
    echo project: $p
    echo version: $version
    echo url:     $url
    echo ref:     $ref
    echo =============================================
    if [[ "$cmd" = clone ]]; then
        clone
    elif [[ "$cmd" = prep ]]; then
        prep
    elif [[ "$cmd" = archive ]]; then
        mkdir -p archive
        rm -f archive/$p.tar archive/$p.tar.xz
        $git -C "downstream/$p" archive --prefix downstream/$p/ upstream-base >archive/$p.tar
    else
        echo "$0: unknown command: $cmd" >&2
        exit 1
    fi
done

if [[ $cmd = archive ]]; then
    echo archive/*.tar | xargs -n1 -P20 xz -9evT8
fi
