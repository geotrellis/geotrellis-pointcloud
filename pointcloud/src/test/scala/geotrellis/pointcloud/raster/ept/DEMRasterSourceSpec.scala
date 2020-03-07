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

import geotrellis.proj4.{CRS, LatLng}
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.{CellSize, Dimensions, DoubleCellType, GridExtent, StringName}
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

      rs.gridExtent shouldBe new GridExtent(Extent(481968.0, 4390186.0, 482856.0, 4391074.0), 6.9375, 6.9375, 128, 128)
      rs.crs shouldBe CRS.fromEpsgCode(26913)

      val res = rs.read()
      res.nonEmpty shouldBe true

      val tile = res.map(_.tile.band(0)).get
      tile.dimensions shouldBe Dimensions(128, 128)
      val (mi, ma) = tile.findMinMaxDouble
      mi shouldBe 1845.971 +- 1e3
      ma shouldBe 2028.893 +- 1e3
    }

    it("should resample RasterSource") {
      val rs = DEMRasterSource(catalog).resample(100, 100)

      rs.metadata shouldBe EPTMetadata(
        StringName("src/test/resources/red-rocks/"),
        CRS.fromEpsgCode(26913),
        DoubleCellType,
        new GridExtent(Extent(481968.0, 4390186.0, 482856.0, 4391074.0), 6.9375, 6.9375, 128, 128),
        List(CellSize(6.9375,6.9375), CellSize(3.46875,3.46875), CellSize(1.734375,1.734375), CellSize(0.8671875,0.8671875), CellSize(0.43359375,0.43359375)),
        Map("points" -> "4004326", "pointsInLevels" -> "15366,186189,465711,2297397,1039663", "minz" -> "1843.0", "maxz" -> "2030.0")
      )

      rs.gridExtent shouldBe new GridExtent(Extent(481968.0, 4390186.0, 482856.0, 4391074.0),8.88, 8.88,100, 100)
      rs.crs shouldBe CRS.fromEpsgCode(26913)

      val res = rs.read()
      res.nonEmpty shouldBe true

      val tile = res.map(_.tile.band(0)).get
      tile.dimensions shouldBe Dimensions(100, 100)
      val (mi, ma) = tile.findMinMaxDouble
      mi shouldBe 1846.696 +- 1e3
      ma shouldBe 2027.657 +- 1e3
    }

    it("should reproject RasterSource") {
      val rs = DEMRasterSource(catalog).reproject(LatLng)

      rs.metadata shouldBe EPTMetadata(
        StringName("src/test/resources/red-rocks/"),
        CRS.fromEpsgCode(26913),
        DoubleCellType,
        new GridExtent(Extent(481968.0, 4390186.0, 482856.0, 4391074.0), 6.9375, 6.9375, 128, 128),
        List(CellSize(6.9375,6.9375), CellSize(3.46875,3.46875), CellSize(1.734375,1.734375), CellSize(0.8671875,0.8671875), CellSize(0.43359375,0.43359375)),
        Map("points" -> "4004326", "pointsInLevels" -> "15366,186189,465711,2297397,1039663", "minz" -> "1843.0", "maxz" -> "2030.0")
      )

      rs.gridExtent shouldBe new GridExtent(Extent(-105.21023644880934, 39.661268543413485, -105.19987676348154, 39.669309977479124), 7.244535194267097E-5,7.244535194267097E-5, 143, 111)
      rs.crs shouldBe LatLng
      val res = rs.read()
      res.nonEmpty shouldBe true

      val tile = res.map(_.tile.band(0)).get
      tile.dimensions shouldBe Dimensions(143, 111)
      val (mi, ma) = tile.findMinMaxDouble
      mi shouldBe 1845.683 +- 1e3
      ma shouldBe 2028.15 +- 1e3
    }

    ignore("rasterizer bug") {
      val ge = new GridExtent[Long](Extent(481968.0, 4390186.0, 482718.32558139536, 4390537.069767442), 6.883720930232645, 6.883720930227462, 109, 51)
      val rs = DEMRasterSource(catalog).resampleToRegion(ge)
      GeoTiff(rs.read().get, rs.crs).write("/tmp/test.tiff")
    }
  }
}
