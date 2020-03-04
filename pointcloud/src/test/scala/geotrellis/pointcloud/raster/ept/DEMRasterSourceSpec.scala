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

import geotrellis.proj4.CRS
import geotrellis.raster.{CellSize, DoubleCellType, GridExtent, StringName}
import geotrellis.vector.Extent

import org.scalatest._

class DEMRasterSourceSpec extends FunSpec with Matchers {
  val catalog: String = "src/test/resources/red-rocks"

  describe("DEMRasterSourceSpec") {
    it("should read from the EPT catalog") {
      val rs = DEMRasterSource(catalog)

      rs.metadata shouldBe EPTMetadata(
        StringName("src/test/resources/red-rocks/"),
        CRS.fromEpsgCode(26913),
        DoubleCellType,
        new GridExtent(Extent(481968.0, 4390186.0, 482856.0, 4391074.0), 6.9375, 6.9375, 128, 128),
        List(CellSize(6.9375,6.9375), CellSize(3.46875,3.46875), CellSize(1.734375,1.734375), CellSize(0.8671875,0.8671875), CellSize(0.43359375,0.43359375)),
        Map("points" -> "4004326", "pointsInLevels" -> "15366,186189,465711,2297397,1039663", "minz" -> "1843.0", "maxz" -> "2030.0")
      )

      val res = rs.read()
      res.nonEmpty shouldBe true
      res.map(_.tile.band(0).findMinMaxDouble) shouldBe Some(1845.9715706168827 -> 2028.8939339826734)
    }
  }
}
