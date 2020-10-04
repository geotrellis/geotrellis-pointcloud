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

package geotrellis.pointcloud.spark.pyramid

import geotrellis.pointcloud.spark._
import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.layer._
import geotrellis.vector._

import org.locationtech.jts.geom.Coordinate

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class PyramidSpec extends AnyFunSpec with Matchers with PointCloudTestEnvironment with PointCloudSpatialTestFiles {
  describe("Pyramid") {
    it("should pyramid Bounds[SpatialKey]") {
      val md = TileLayerMetadata(
        ByteCellType,
        LayoutDefinition(
          Extent(-2.0037508342789244E7, -2.0037508342789244E7,
            2.0037508342789244E7, 2.0037508342789244E7),
          TileLayout(8192,8192,256,256)
        ),
        Extent(-9634947.090382002, 4024185.376428919,
          -9358467.589532925, 4300664.877277998),
        WebMercator,
        KeyBounds(SpatialKey(2126,3216),SpatialKey(2182,3273))
      )

      val scheme =  ZoomedLayoutScheme(WebMercator, 256)
      var rdd = ContextRDD(sc.emptyRDD[(SpatialKey, Array[Coordinate])], md)
      var zoom: Int = 13

      while (zoom > 0) {
        val (newZoom, newRDD) = Pyramid.up(rdd, scheme, zoom)
        val previousExtent = rdd.metadata.mapTransform(rdd.metadata.bounds.get.toGridBounds())
        val nextExtent = newRDD.metadata.mapTransform(newRDD.metadata.bounds.get.toGridBounds())
        nextExtent.contains(previousExtent) should be (true)
        zoom = newZoom
        rdd = newRDD
      }
    }
  }
}
