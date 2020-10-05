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

package geotrellis.pointcloud.raster.rasterize.triangles

import geotrellis.raster._
import geotrellis.vector._

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class TrianglesRasterizerSpec extends AnyFunSpec with Matchers {

  describe("TrianglesRasterizer") {

    val points = Array(
      Point(0, 2), Point(1, 2), Point(2, 2),
      Point(2, 1), Point(1, 1), Point(0, 1),
      Point(0, 0), Point(1, 0), Point(2, 0))
    val zs = Array[Double](0, 1, 2, 3, 4, 5, 6, 7, 8)
    val triangles = Array(
      Polygon(points(1), points(4), points(5), points(1)),
      Polygon(points(1), points(2), points(4), points(1)),
      Polygon(points(4), points(2), points(3), points(4)),
      Polygon(points(6), points(5), points(4), points(6)),
      Polygon(points(6), points(4), points(7), points(6)))
    val re = RasterExtent(
      Extent(0, 0, 2, 2),
      2, 2
    )
    val indexMap = points.map({ point => (point.x, point.y) }).zipWithIndex.toMap

    it("should work when pixel falls on boundary of two triangles") {
      val tile =
        TrianglesRasterizer(
          re,
          (0 to 8).map({ i => i.toDouble }).toArray,
          triangles,
          indexMap)

      tile.getDouble(0, 1) should be ((6.0 + 4.0) / 2)
    }

    it("should work when pixel falls on boundary of only one triangle") {
      val tile =
        TrianglesRasterizer(
          re,
          (0 to 8).map({ i => i.toDouble }).toArray,
          triangles,
          indexMap)

      tile.getDouble(0, 0) should be ((1.0 + 5.0) / 2)
    }

    it("should work when pixel falls in no triangles") {
      val tile =
        TrianglesRasterizer(
          re,
          (0 to 8).map({ i => i.toDouble }).toArray,
          triangles,
          indexMap)

      java.lang.Double.isNaN(tile.getDouble(1, 1)) should be (true)
    }

    it("should work when pixel falls in the interior of one triangle") {
      val tile =
        TrianglesRasterizer(
          RasterExtent(Extent(0.1, 0, 2, 2), 2, 2),
          (0 to 8).map({ i => i.toDouble }).toArray,
          triangles,
          indexMap)

      tile.getDouble(0, 0) should be (2.925)
    }

  }
}
