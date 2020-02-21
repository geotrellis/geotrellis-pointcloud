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

import scala.util.Properties

object Environment {
  def either(environmentVariable: String, default: String): String =
    Properties.envOrElse(environmentVariable, default)

  lazy val hadoopVersion  = either("SPARK_HADOOP_VERSION", "2.8.5")
  lazy val sparkVersion   = either("SPARK_VERSION", "2.4.5")
  lazy val versionSuffix  = either("VERSION_SUFFIX", "-SNAPSHOT")
  lazy val ldLibraryPath  = either("LD_LIBRARY_PATH", "/usr/local/lib")
}
