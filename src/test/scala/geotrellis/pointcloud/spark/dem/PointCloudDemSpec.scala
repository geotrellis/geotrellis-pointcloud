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

package geotrellis.pointcloud.spark.dem

import geotrellis.raster._
import geotrellis.pointcloud.spark.buffer._
import geotrellis.pointcloud.spark.io.hadoop.HadoopPointCloudRDD
import geotrellis.pointcloud.spark.{PointCloudTestEnvironment, _}
import geotrellis.spark.tiling._
import geotrellis.vector.Extent

import org.scalatest._

class PointCloudDemSpec extends FunSpec
  with Matchers
  with PointCloudTestEnvironment {

  describe("PointCloud DEM support") {

    val min = { (a: Double, b: Double) => math.min(a, b) }
    val max = { (a: Double, b: Double) => math.max(a, b) }
    val rdd = HadoopPointCloudRDD(lasPath).flatMap(_._2)
    val cloud = rdd.first

    it("should be able to union two clouds") {
      val clouds = cloud.union(cloud)

      clouds.length should be (cloud.length * 2)
    }

    it("should be able to produce a tile") {
      val length = cloud.length
      val xs = (0 until length).map({ i => cloud.getDouble(i, "X") })
      val ys = (0 until length).map({ i => cloud.getDouble(i, "Y") })
      val xmin = xs.reduce(min)
      val xmax = xs.reduce(max)
      val ymin = ys.reduce(min)
      val ymax = ys.reduce(max)

      val re = RasterExtent(Extent(xmin, ymin, xmax, ymax), 10, 10)

      val tile = cloud.toTile(re, "Z")

      tile.getDouble(0, 0) should be < 420.51 // 435.50
      tile.getDouble(0, 0) should be > 420.50 // 435.49
    }

    it("should work with BufferUnionable") {
      val layoutDefinition = LayoutDefinition(
        Extent(635609.85, 848889.7, 638992.55, 853545.43),
        TileLayout(layoutCols = 5, layoutRows = 5, tileCols = 10, tileRows = 10))
      val tiled = rdd.tileToLayout(layoutDefinition)
      val buffered = BufferUnionable(tiled)

      tiled.count should be (buffered.count)

      val result = tiled
        .zip(buffered)
        .map({ case ((_, a), (_, b)) => a.length < b.length })
        .reduce(_ && _)

      result should be (true)
    }

  }
}
