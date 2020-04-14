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
import org.scalatest._

class TINRasterSourceSpec extends FunSpec with RasterMatchers {
  val catalog: String = "src/test/resources/red-rocks"

  describe("TINRasterSourceSpec") {
    it("should read from the EPT catalog") {
      val expectedMetadata: EPTMetadata = EPTMetadata(
        name        = "src/test/resources/red-rocks/",
        crs         = CRS.fromEpsgCode(26913),
        cellType    = DoubleCellType,
        gridExtent  = new GridExtent(Extent(481968.0, 4390186.0, 482856.0, 4391074.0), 0.216796875, 0.216796875, 4096, 4096),
        resolutions = List(CellSize(0.216796875,0.216796875), CellSize(0.43359375,0.43359375), CellSize(0.8671875,0.8671875), CellSize(1.734375,1.734375), CellSize(3.46875,3.46875), CellSize(6.9375,6.9375)),
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
        gridExtent  = new GridExtent(Extent(481968.0, 4390186.0, 482856.0, 4391074.0), 0.216796875, 0.216796875, 4096, 4096),
        resolutions = List(CellSize(0.216796875,0.216796875), CellSize(0.43359375,0.43359375), CellSize(0.8671875,0.8671875), CellSize(1.734375,1.734375), CellSize(3.46875,3.46875), CellSize(6.9375,6.9375)),
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
        gridExtent  = new GridExtent(Extent(481968.0, 4390186.0, 482856.0, 4391074.0),8.88, 8.88,100, 100),
        resolutions = List(CellSize(0.216796875,0.216796875), CellSize(0.43359375,0.43359375), CellSize(0.8671875,0.8671875), CellSize(1.734375,1.734375), CellSize(3.46875,3.46875), CellSize(6.9375,6.9375)),
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
      mi shouldBe 1846.6 +- 1e-1
      ma shouldBe 2027.6 +- 1e-1
    }

    it("should reproject RasterSource") {
      val expectedMetadata: EPTMetadata = EPTMetadata(
        name        = "src/test/resources/red-rocks/",
        crs         = LatLng,
        cellType    = DoubleCellType,
        gridExtent  = new GridExtent(Extent(-105.21023644880934, 39.66129118258597, -105.1998609160608, 39.669309977479124), 2.263917248208468E-6, 2.263917248208468E-6,4583, 3542),
        resolutions = List(CellSize(2.263917248208468E-6,2.263917248208468E-6), CellSize(4.527834496416936E-6,4.527834496416936E-6), CellSize(9.055668992833871E-6,9.055668992833871E-6), CellSize(1.8111337985667743E-5,1.8111337985667743E-5), CellSize(3.6222675971335486E-5,3.6222675971335486E-5), CellSize(7.244535194267097E-5,7.244535194267097E-5)),
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
    ignore("reprojection bug") {
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
