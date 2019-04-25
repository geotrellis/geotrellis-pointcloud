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

package geotrellis.pointcloud

import geotrellis.pointcloud.vector.{Extent3D, ProjectedExtent3D}
import geotrellis.spark.{Metadata, SpatialKey, TileLayerMetadata}
import geotrellis.spark.tiling.TilerKeyMethods
import geotrellis.util._

import org.apache.spark.rdd.RDD
import org.locationtech.jts.geom.Coordinate

package object spark extends dem.Implicits with tiling.Implicits with Serializable {
  type PointCloudLayerRDD[K] = RDD[(SpatialKey, Array[Coordinate])] with Metadata[TileLayerMetadata[K]]

  implicit class withProjectedExtent3DTilerKeyMethods[K: Component[?, ProjectedExtent3D]](val self: K) extends TilerKeyMethods[K, SpatialKey] {
    def extent = self.getComponent[ProjectedExtent3D].extent3d.toExtent
    def translate(spatialKey: SpatialKey) = spatialKey
  }
}
