#!/usr/bin/env bash

git clone https://github.com/pomadchin/geotrellis
cd geotrellis
git checkout feature/delaunay-public
./scripts/publish-local.sh
