/*
 * Copyright 2020 Azavea
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

package geotrellis.pointcloud.raster.ept

import org.scalatest.funspec.AnyFunSpec

class EPTMetadataSpec extends AnyFunSpec {
  val catalog: String = "src/test/resources/red-rocks"

  describe("EPTMetadata") {
    it("must have sorted resolutions") {
      val md = EPTMetadata(catalog)
      val rs = md.resolutions.map(_.resolution)
      val diffs = rs.zip(rs.drop(1)).map{ case (a,b) => b - a }

      assert(diffs.forall(_ > 0))
    }
  }
}
