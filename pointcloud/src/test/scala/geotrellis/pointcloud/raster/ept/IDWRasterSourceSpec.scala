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

import geotrellis.layer._
import geotrellis.proj4.{CRS, LatLng, WebMercator}
import geotrellis.raster.geotiff.GeoTiffRasterSource
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.raster.testkit.RasterMatchers
import geotrellis.raster.{CellSize, DefaultTarget, Dimensions, DoubleCellType, GridExtent, Raster, TileLayout, TargetDimensions}
import geotrellis.vector.Extent

import org.scalatest._

class IDWRasterSourceSpec extends FunSpec with RasterMatchers {
  val catalog: String = "src/test/resources/red-rocks"

  describe("IDWRasterSourceSpec") {
    it("should read from the EPT catalog") {
      val expectedMetadata: EPTMetadata = EPTMetadata(
        name        = "src/test/resources/red-rocks/",
        crs         = CRS.fromEpsgCode(26913),
        cellType    = DoubleCellType,
        gridExtent  = new GridExtent(Extent(481968.0, 4390186.0, 482856.0, 4391074.0), 0.216796875, 0.216796875, 4096, 4096),
        resolutions = List(CellSize(0.216796875,0.216796875),CellSize(0.43359375,0.43359375), CellSize(0.8671875,0.8671875), CellSize(1.734375,1.734375), CellSize(3.46875,3.46875), CellSize(6.9375,6.9375)),
        attributes  = Map("points" -> "4004326", "pointsInLevels" -> "", "minz" -> "1843.0", "maxz" -> "2030.0")
      )

      val rs = IDWRasterSource(catalog, TargetDimensions(128, 128))

      rs.metadata shouldBe expectedMetadata
      rs.gridExtent shouldBe new GridExtent(Extent(481968.0, 4390186.0, 482856.0, 4391074.0), 6.9375, 6.9375, 128, 128) // expectedMetadata.gridExtent
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
        gridExtent  = new GridExtent(Extent(481968.0, 4390186.0, 482856.0, 4391074.0),8.88, 8.88, 100, 100),
        resolutions = List(CellSize(0.216796875,0.216796875), CellSize(0.43359375,0.43359375), CellSize(0.8671875,0.8671875), CellSize(1.734375,1.734375), CellSize(3.46875,3.46875), CellSize(6.9375,6.9375)),
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
        gridExtent  = new GridExtent(Extent(-105.21023644880934, 39.66129118258597, -105.1998609160608, 39.669309977479124), 7.255617306671465E-5, 7.224139543381823E-5,143, 111),
        resolutions = List(CellSize(0.216796875,0.216796875), CellSize(0.43359375,0.43359375), CellSize(0.8671875,0.8671875), CellSize(1.734375,1.734375), CellSize(3.46875,3.46875), CellSize(6.9375,6.9375)),
        attributes  = Map("points" -> "4004326", "pointsInLevels" -> "", "minz" -> "1843.0", "maxz" -> "2030.0")
      )

      val rs = IDWRasterSource(catalog).reproject(LatLng).resample(143, 111, NearestNeighbor, geotrellis.raster.io.geotiff.Auto(0))

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
