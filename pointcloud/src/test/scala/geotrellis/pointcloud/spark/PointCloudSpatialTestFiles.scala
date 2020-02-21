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

package geotrellis.pointcloud.spark

import geotrellis.pointcloud.spark.store.hadoop._
import geotrellis.proj4.CRS
import geotrellis.raster.{DoubleConstantNoDataCellType, TileLayout}
import geotrellis.spark.ContextRDD
import geotrellis.layer._
import geotrellis.vector.Extent

import spire.syntax.cfor.cfor
import org.locationtech.jts.geom.Coordinate

import scala.collection.mutable

trait PointCloudSpatialTestFiles extends Serializable { self: PointCloudTestEnvironment =>
  val extent = Extent(635609.85, 848889.7, 638992.55, 853545.43)
  val crs = CRS.fromEpsgCode(20255)
  val rdd = HadoopPointCloudRDD(lasPath).flatMap(_._2)
  val layoutDefinition = LayoutDefinition(
    extent,
    TileLayout(layoutCols = 5, layoutRows = 5, tileCols = 10, tileRows = 10))
  val tiledWithLayout = rdd.tileToLayout(layoutDefinition)
  val gb = layoutDefinition.mapTransform(extent)

  val md =
    TileLayerMetadata[SpatialKey](
      cellType = DoubleConstantNoDataCellType,
      layout = layoutDefinition,
      extent = extent,
      crs = crs,
      bounds = KeyBounds(gb)
    )

  val pointCloudSample = ContextRDD(tiledWithLayout, md)

  val rddc = HadoopPointCloudRDD(lasPath).flatMap { case (_, pointClouds) =>
    val extent = Extent(635609.85, 848889.7, 638992.55, 853545.43)
    val layoutDefinition = LayoutDefinition(
      extent,
      TileLayout(layoutCols = 5, layoutRows = 5, tileCols = 10, tileRows = 10))
    val mapTransform = layoutDefinition.mapTransform

    var lastKey: SpatialKey = null
    val keysToPoints = mutable.Map[SpatialKey, mutable.ArrayBuffer[Coordinate]]()

    for (pointCloud <- pointClouds) {
      val len = pointCloud.length
      cfor(0)(_ < len, _ + 1) { i =>
        val x = pointCloud.getX(i)
        val y = pointCloud.getY(i)
        val z = pointCloud.getZ(i)
        val p = new Coordinate(x, y, z)
        val key = mapTransform(x, y)
        if (key == lastKey) {
          keysToPoints(lastKey) += p
        } else if (keysToPoints.contains(key)) {
          keysToPoints(key) += p
          lastKey = key
        } else {
          keysToPoints(key) = mutable.ArrayBuffer(p)
          lastKey = key
        }
      }
    }

    keysToPoints.map { case (k, v) => (k, v.toArray) }
  }
  .reduceByKey(_ ++ _).filter { _._2.length > 2 }

  val pointCloudSampleC = ContextRDD(rddc, md)

}
