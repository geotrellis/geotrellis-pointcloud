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
import geotrellis.raster.io.geotiff.Auto
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.raster.testkit.RasterMatchers
import geotrellis.raster.{CellSize, DefaultTarget, Dimensions, DoubleCellType, GridExtent, Raster, TileLayout}
import geotrellis.vector.Extent

import org.scalatest.funspec.AnyFunSpec

class TINRasterSourceSpec extends AnyFunSpec with RasterMatchers {
  val catalog: String = "src/test/resources/red-rocks"

  describe("TINRasterSourceSpec") {
    it("should read from the EPT catalog") {
      val expectedMetadata: EPTMetadata = EPTMetadata(
        name        = "src/test/resources/red-rocks/",
        crs         = CRS.fromEpsgCode(26913),
        cellType    = DoubleCellType,
        gridExtent  = new GridExtent(Extent(481969.0, 4390186.0, 482855.0, 4391072.0), 0.21630859375,0.21630859375, 4096, 4096),
        resolutions = List(CellSize(0.21630859375,0.21630859375), CellSize(0.4326171875,0.4326171875), CellSize(0.865234375,0.865234375), CellSize(1.73046875,1.73046875), CellSize(3.4609375,3.4609375), CellSize(6.921875,6.921875)),
        attributes  = Map("points" -> "4004326", "pointsInLevels" -> "", "minz" -> "1843.0", "maxz" -> "2030.0")
      )

      val rs = TINRasterSource(catalog)

      rs.metadata shouldBe expectedMetadata
      rs.gridExtent shouldBe expectedMetadata.gridExtent
      rs.crs shouldBe expectedMetadata.crs
      rs.resolutions shouldBe expectedMetadata.resolutions

      val res = rs.read()
      res.nonEmpty shouldBe true

      val tile = res.map(_.tile.band(0)).get
      tile.dimensions shouldBe Dimensions(4096, 4096)
      val (mi, ma) = tile.findMinMaxDouble

      // threshold is large, since triangulation mesh can vary a little that may cause
      // slightly different results during the rasterization process
      // mi shouldBe 1845.9 +- 1e-1
      // ma shouldBe 2028.9 +- 1e-1

      (mi >= rs.metadata.attributes("minz").toDouble) shouldBe true
      (ma <= rs.metadata.attributes("maxz").toDouble) shouldBe true
    }

    it("should read from the EPT catalog at a less resolute level of detail") {
      val expectedMetadata: EPTMetadata = EPTMetadata(
        name        = "src/test/resources/red-rocks/",
        crs         = CRS.fromEpsgCode(26913),
        cellType    = DoubleCellType,
        gridExtent  = new GridExtent(Extent(481969.0, 4390186.0, 482855.0, 4391072.0), 0.21630859375, 0.21630859375, 4096, 4096),
        resolutions = List(CellSize(0.21630859375,0.21630859375), CellSize(0.4326171875,0.4326171875), CellSize(0.865234375,0.865234375), CellSize(1.73046875,1.73046875), CellSize(3.4609375,3.4609375), CellSize(6.921875,6.921875)),
        attributes  = Map("points" -> "4004326", "pointsInLevels" -> "", "minz" -> "1843.0", "maxz" -> "2030.0")
      )

      val rs = TINRasterSource(catalog, overviewStrategy = Auto(6))

      rs.metadata shouldBe expectedMetadata
      rs.gridExtent shouldBe expectedMetadata.gridExtent
      rs.crs shouldBe expectedMetadata.crs
      rs.resolutions shouldBe expectedMetadata.resolutions

      val res = rs.read()
      res.nonEmpty shouldBe true

      val tile = res.map(_.tile.band(0)).get
      tile.dimensions shouldBe Dimensions(4096, 4096)
      val (mi, ma) = tile.findMinMaxDouble

      // threshold is large, since triangulation mesh can vary a little that may cause
      // slightly different results during the rasterization process
      // mi shouldBe 1845.9 +- 1e-1
      // ma shouldBe 2028.9 +- 1e-1

      (mi >= rs.metadata.attributes("minz").toDouble) shouldBe true
      (ma <= rs.metadata.attributes("maxz").toDouble) shouldBe true
    }

    it("should resample RasterSource") {
      val expectedMetadata: EPTMetadata = EPTMetadata(
        name        = "src/test/resources/red-rocks/",
        crs         = CRS.fromEpsgCode(26913),
        cellType    = DoubleCellType,
        gridExtent  = new GridExtent(Extent(481969.0, 4390186.0, 482855.0, 4391072.0),8.86, 8.86,100, 100),
        resolutions = List(CellSize(0.21630859375,0.21630859375), CellSize(0.4326171875,0.4326171875), CellSize(0.865234375,0.865234375), CellSize(1.73046875,1.73046875), CellSize(3.4609375,3.4609375), CellSize(6.921875,6.921875)),
        attributes  = Map("points" -> "4004326", "pointsInLevels" -> "", "minz" -> "1843.0", "maxz" -> "2030.0")
      )

      val rs = TINRasterSource(catalog).resample(100, 100)

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
      mi shouldBe 1846.6 +- 3e-1
      ma shouldBe 2027.6 +- 3e-1
    }

    it("should reproject RasterSource") {
      val expectedMetadata: EPTMetadata = EPTMetadata(
        name        = "src/test/resources/red-rocks/",
        crs         = LatLng,
        cellType    = DoubleCellType,
        gridExtent  = new GridExtent(Extent(-105.21022473519201, 39.66129120299395, -105.19987257160376, 39.66929193688651), 2.258818151484163E-6, 2.258818151484163E-6,4583, 3542),
        resolutions = List(CellSize(2.258818151484163E-6,2.258818151484163E-6), CellSize(4.517636302968326E-6,4.517636302968326E-6), CellSize(9.035272605936652E-6,9.035272605936652E-6), CellSize(1.8070545211873303E-5,1.8070545211873303E-5), CellSize(3.614109042374661E-5,3.614109042374661E-5), CellSize(7.228218084749321E-5,7.228218084749321E-5)),
        attributes  = Map("points" -> "4004326", "pointsInLevels" -> "", "minz" -> "1843.0", "maxz" -> "2030.0")
      )

      val rs = TINRasterSource(catalog).reproject(LatLng)

      rs.metadata shouldBe expectedMetadata
      rs.gridExtent shouldBe expectedMetadata.gridExtent
      rs.crs shouldBe LatLng

      val res = rs.read()
      res.nonEmpty shouldBe true

      val tile = res.map(_.tile.band(0)).get
      tile.dimensions shouldBe Dimensions(4583, 3542)
      val (mi, ma) = tile.findMinMaxDouble

      // // threshold is large, since triangulation mesh can vary a little that may cause
      // // slightly different results during the rasterization process
      // mi shouldBe 1845.6 +- 2
      // ma shouldBe 2026.7 +- 2

      (mi >= rs.metadata.attributes("minz").toDouble) shouldBe true
      (ma <= rs.metadata.attributes("maxz").toDouble) shouldBe true
    }

    // https://github.com/geotrellis/geotrellis-pointcloud/issues/47
    it("rasterizer bug") {
      val ge = new GridExtent[Long](Extent(481968.0, 4390186.0, 482718.32558139536, 4390537.069767442), 6.883720930232645, 6.883720930227462, 109, 51)
      val rs = TINRasterSource(catalog).resampleToRegion(ge)

      val actual = rs.read().get

      val ers = GeoTiffRasterSource("src/test/resources/tiff/dem-rasterizer-bug.tiff")
      val expected = ers.read().get

      // threshold is large, since triangulation mesh can vary a little that may cause
      // slightly different results during the rasterization process
      assertRastersEqual(actual, expected, 1e1)
      ers.crs shouldBe rs.crs
    }

    // https://github.com/geotrellis/geotrellis-pointcloud/issues/47
    it("reprojection bug") {
      // NOTE: This test fails because of a small number (2–4) of pixels
      // around the boundary that are NODATA in one image, but have a value
      // in the other.  Visual inspection confirms that the images match
      // otherwise.

      val key = SpatialKey(27231, 49781)
      val ld = LayoutDefinition(
        Extent(-2.003750834278925E7, -2.003750834278925E7, 2.003750834278925E7, 2.003750834278925E7),
        TileLayout(131072, 131072, 256, 256)
      )

      val rs =
        TINRasterSource(catalog)
          .reproject(WebMercator, DefaultTarget)
          .tileToLayout(ld, NearestNeighbor)

      val actual = Raster(rs.read(key).get, ld.mapTransform(key))

      // import geotrellis.raster.io.geotiff.SinglebandGeoTiff
      // val geotiff = SinglebandGeoTiff(
      //     actual.tile.band(0),
      //     actual.extent,
      //     WebMercator
      //   )
      // geotiff.write("/data/dem-reprojection-bug-actual.tiff")

      val ers = GeoTiffRasterSource("src/test/resources/tiff/dem-reprojection-bug.tiff")
      val expected = ers.read().get

      // threshold is large, since triangulation mesh can vary a little that may cause
      // slightly different results during the rasterization process
      assertRastersEqual(actual, expected, 1e1)
      ers.crs shouldBe WebMercator
    }
  }
}
