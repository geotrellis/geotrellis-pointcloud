#!/usr/bin/env bash
./sbt -J-Xmx2G "++$TRAVIS_SCALA_VERSION" test  || { exit 1; }
