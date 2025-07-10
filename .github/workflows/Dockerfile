#
# Copyright (c) 2021-2022 Red Hat, Inc
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

FROM registry.fedoraproject.org/fedora:rawhide

ENV LC_ALL=C.utf8 \
    JAVA_HOME=/usr/lib/jvm/java-25

RUN dnf -y update \
 && dnf -y install git-core \
                   unzip \
                   java-25-devel \
                   byaccj \
                   gcc \
                   rpm-devel \
                   jurand \
 && dnf -y clean all \
 && git config --global user.email root@localhost \
 && git config --global user.name root

RUN git clone https://github.com/fedora-java/javapackages-bootstrap.git /tmp/git \
 && cd /tmp/git \
 && ./downstream.sh clone \
 && mv /tmp/git/upstream /var/cache \
 && rm -f /var/cache/upstream/*.zip \
 && rm -rf /tmp/git
