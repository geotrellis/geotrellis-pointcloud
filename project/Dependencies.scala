/*
 * Copyright (c) 2017 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import sbt._

object Version {
  val geotrellisPointCloud = "0.3.1" + Environment.versionSuffix
  val geotrellis           = "3.2.0"
  val crossScala           = List("2.12.10", "2.11.12")
  val scala                = crossScala.head
  val pdal                 = "2.1.2"
  val scalaTest            = "3.0.8"
  lazy val hadoop          = Environment.hadoopVersion
  lazy val spark           = Environment.sparkVersion
}

object Dependencies {
  val scalatest    = "org.scalatest"    %%  "scalatest"    % Version.scalaTest
  val sparkCore    = "org.apache.spark" %% "spark-core"    % Version.spark
  val sparkSQL     = "org.apache.spark" %% "spark-sql"     % Version.spark
  val hadoopClient = "org.apache.hadoop" % "hadoop-client" % Version.hadoop
  val hadoopAWS    = "org.apache.hadoop" % "hadoop-aws" % Version.hadoop
  
  val pdalScala    = "io.pdal" %% "pdal-scala" % Version.pdal
  val pdalNative   = "io.pdal" % "pdal-native" % Version.pdal
  
  val geotrellisSparkTestkit = "org.locationtech.geotrellis" %% "geotrellis-spark-testkit" % Version.geotrellis
  val geotrellisSpark        = "org.locationtech.geotrellis" %% "geotrellis-spark" % Version.geotrellis
  val geotrellisRaster       = "org.locationtech.geotrellis" %% "geotrellis-raster" % Version.geotrellis
  val geotrellisS3           = "org.locationtech.geotrellis" %% "geotrellis-s3" % Version.geotrellis
  val geotrellisS3Spark      = "org.locationtech.geotrellis" %% "geotrellis-s3-spark" % Version.geotrellis
  val geotrellisS3Testkit    = "org.locationtech.geotrellis" %% "geotrellis-s3-testkit" % Version.geotrellis
}
