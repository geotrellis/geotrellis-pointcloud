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
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.raster.testkit.RasterMatchers
import geotrellis.raster.{CellSize, Dimensions, DoubleCellType, GridExtent, TargetDimensions}
import geotrellis.vector.Extent

import org.scalatest.funspec.AnyFunSpec

class IDWRasterSourceSpec extends AnyFunSpec with RasterMatchers {
  val catalog: String = "src/test/resources/red-rocks"

  describe("IDWRasterSourceSpec") {
    it("should read from the EPT catalog") {
      val expectedMetadata: EPTMetadata = EPTMetadata(
        name        = "src/test/resources/red-rocks/",
        crs         = CRS.fromEpsgCode(26913),
        cellType    = DoubleCellType,
        gridExtent  = new GridExtent(Extent(481969.0, 4390186.0, 482855.0, 4391072.0), 0.21630859375,0.21630859375, 4096, 4096),
        resolutions = List(CellSize(0.21630859375,0.21630859375), CellSize(0.4326171875,0.4326171875), CellSize(0.865234375,0.865234375), CellSize(1.73046875,1.73046875), CellSize(3.4609375,3.4609375), CellSize(6.921875,6.921875)),
        attributes  = Map("points" -> "4004326", "pointsInLevels" -> "", "minz" -> "1843.0", "maxz" -> "2030.0")
      )

      val rs = IDWRasterSource(catalog, TargetDimensions(128, 128))

      rs.metadata shouldBe expectedMetadata
      rs.gridExtent shouldBe new GridExtent(Extent(481969.0, 4390186.0, 482855.0, 4391072.0), 6.921875, 6.921875, 128, 128) // expectedMetadata.gridExtent
      rs.crs shouldBe expectedMetadata.crs

      val res = rs.read()
      res.nonEmpty shouldBe true

      val tile = res.map(_.tile.band(0)).get
      tile.dimensions shouldBe Dimensions(128, 128)
      val (mi, ma) = tile.findMinMaxDouble

      // threshold is large, since triangulation mesh can vary a little that may cause
      // slightly different results during the rasterization process
      (mi >= rs.metadata.attributes("minz").toDouble) shouldBe true
      (ma <= rs.metadata.attributes("maxz").toDouble) shouldBe true
    }

    it("should resample RasterSource") {
      val expectedMetadata: EPTMetadata = EPTMetadata(
        name        = "src/test/resources/red-rocks/",
        crs         = CRS.fromEpsgCode(26913),
        cellType    = DoubleCellType,
        gridExtent  = new GridExtent(Extent(481969.0, 4390186.0, 482855.0, 4391072.0),8.86, 8.86, 100, 100),
        resolutions = List(CellSize(0.21630859375,0.21630859375), CellSize(0.4326171875,0.4326171875), CellSize(0.865234375,0.865234375), CellSize(1.73046875,1.73046875), CellSize(3.4609375,3.4609375), CellSize(6.921875,6.921875)),
        attributes  = Map("points" -> "4004326", "pointsInLevels" -> "", "minz" -> "1843.0", "maxz" -> "2030.0")
      )

      val rs = IDWRasterSource(catalog).resample(100, 100)

      rs.metadata shouldBe expectedMetadata
      rs.gridExtent shouldBe expectedMetadata.gridExtent
      rs.crs shouldBe expectedMetadata.crs

      val res = rs.read()
      res.nonEmpty shouldBe true

      val tile = res.map(_.tile.band(0)).get
      tile.dimensions shouldBe Dimensions(100, 100)
      val (mi, ma) = tile.findMinMaxDouble

      // threshold is large, since triangulation mesh can vary a little that may cause
      // slightly different results during the rasterization process
      (mi >= rs.metadata.attributes("minz").toDouble) shouldBe true
      (ma <= rs.metadata.attributes("maxz").toDouble) shouldBe true
    }

    it("should reproject RasterSource") {
      val expectedMetadata: EPTMetadata = EPTMetadata(
        name        = "src/test/resources/red-rocks/",
        crs         = CRS.fromEpsgCode(26913),
        cellType    = DoubleCellType,
        gridExtent  = new GridExtent(Extent(-105.21022473519201, 39.66129120299395, -105.19987257160376, 39.66929193688651), 7.23927523653953E-5, 7.207868371674133E-5,143, 111),
        resolutions = List(CellSize(0.21630859375,0.21630859375), CellSize(0.4326171875,0.4326171875), CellSize(0.865234375,0.865234375), CellSize(1.73046875,1.73046875), CellSize(3.4609375,3.4609375), CellSize(6.921875,6.921875)),
        attributes  = Map("points" -> "4004326", "pointsInLevels" -> "", "minz" -> "1843.0", "maxz" -> "2030.0")
      )

      val rs = IDWRasterSource(catalog).reproject(LatLng).resample(143, 111, NearestNeighbor, geotrellis.raster.io.geotiff.Auto(2))

      rs.metadata shouldBe expectedMetadata
      rs.gridExtent shouldBe expectedMetadata.gridExtent
      rs.crs shouldBe LatLng

      val res = rs.read()
      res.nonEmpty shouldBe true

      val tile = res.map(_.tile.band(0)).get
      tile.dimensions shouldBe Dimensions(143, 111)
      val (mi, ma) = tile.findMinMaxDouble

      // threshold is large, since triangulation mesh can vary a little that may cause
      // slightly different results during the rasterization process
      (mi >= rs.metadata.attributes("minz").toDouble) shouldBe true
      (ma <= rs.metadata.attributes("maxz").toDouble) shouldBe true
    }
  }
}
