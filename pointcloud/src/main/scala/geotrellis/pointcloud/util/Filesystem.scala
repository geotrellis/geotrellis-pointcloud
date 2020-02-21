/*
 * Copyright 2017 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.pointcloud.util

import java.io._
import java.util.UUID

object Filesystem {
  /**
    * Create a directory inside the given parent directory. The directory is guaranteed to be
    * newly created, and is not marked for automatic deletion.
    * Function design took from spark.util.Utils.scala.
    *
    * @param root The root path where to create temporary directory
    * @param namePrefix prefix of the created dir
    * @param maxDirCreationAttempts max attempts to create tmp dir
    */
  def createDirectory(root: String = System.getProperty("java.io.tmpdir"), namePrefix: String = "spark", maxDirCreationAttempts: Int = 10): File = {
    var attempts = 0
    val maxAttempts = maxDirCreationAttempts
    var dir: File = null
    while (dir == null) {
      attempts += 1
      if (attempts > maxAttempts) {
        throw new IOException("Failed to create a temp directory (under " + root + ") after " +
          maxAttempts + " attempts!")
      }
      try {
        dir = new File(root, namePrefix + "-" + UUID.randomUUID.toString)
        if (dir.exists() || !dir.mkdirs()) {
          dir = null
        }
      } catch { case e: SecurityException => dir = null; }
    }

    dir
  }
}
