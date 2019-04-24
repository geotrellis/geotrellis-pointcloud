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

object Version {
  val geotrellisPointCloud = "0.2.10" + Environment.versionSuffix
  val geotrellis           = "2.2.0"
  val scala                = "2.11.12"
  val pdal                 = "1.8.5"
  val scalaTest            = "3.0.7"
  lazy val hadoop          = Environment.hadoopVersion
  lazy val spark           = Environment.sparkVersion
}
