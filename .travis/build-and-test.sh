#!/usr/bin/env bash
./sbt -J-Xmx2G "++$TRAVIS_SCALA_VERSION" "project pointcloud" test  || { exit 1; }
