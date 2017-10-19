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

object Dependencies {
  val scalatest    = "org.scalatest"    %%  "scalatest"      % "3.0.4"
  val sparkCore    = "org.apache.spark" %% "spark-core" % Version.spark
  val hadoopClient = "org.apache.hadoop" % "hadoop-client" % Version.hadoop
  val pdalScala    = "io.pdal" %% "pdal-scala" % "1.6.0"
  
  val geotrellisSparkTestkit = "org.locationtech.geotrellis" %% "geotrellis-spark-testkit" % Version.geotrellis
  val geotrellisSpark        = "org.locationtech.geotrellis" %% "geotrellis-spark" % Version.geotrellis
  val geotrellisRaster       = "org.locationtech.geotrellis" %% "geotrellis-raster" % Version.geotrellis
  val geotrellisS3           = "org.locationtech.geotrellis" %% "geotrellis-s3" % Version.geotrellis
  val geotrellisS3Testkit    = "org.locationtech.geotrellis" %% "geotrellis-s3-testkit" % Version.geotrellis
}
