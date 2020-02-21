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

package geotrellis.pointcloud.layer

import geotrellis.layer._
import geotrellis.util._

import io.circe.generic.JsonCodec

// --- //

/** A three-dimensional spatial key. A ''voxel'' is the 3D equivalent of a pixel. */
@JsonCodec
case class VoxelKey(col: Int, row: Int, layer: Int) {
  def spatialKey = SpatialKey(col, row)
  def depthKey = DepthKey(layer)
}

/** Typeclass instances. These (particularly [[Boundable]]) are necessary
  * for when a layer's key type is parameterized as ''K''.
  */
object VoxelKey {
  implicit def ordering[A <: VoxelKey]: Ordering[A] =
    Ordering.by(k => (k.col, k.row, k.layer))

  implicit object Boundable extends Boundable[VoxelKey] {
    def minBound(a: VoxelKey, b: VoxelKey) = {
      VoxelKey(math.min(a.col, b.col), math.min(a.row, b.row), math.min(a.layer, b.layer))
    }

    def maxBound(a: VoxelKey, b: VoxelKey) = {
      VoxelKey(math.max(a.col, b.col), math.max(a.row, b.row), math.max(a.layer, b.layer))
    }
  }

  /** Since [[VoxelKey]] has x and y coordinates, it can take advantage of
    * the [[SpatialComponent]] lens. Lenses are essentially "getters and setters"
    * that can be used in highly generic code.
    */
  implicit val spatialComponent: Component[VoxelKey, SpatialKey] = {
    Component[VoxelKey, SpatialKey](
      /* "get" a SpatialKey from VoxelKey */
      k => SpatialKey(k.col, k.row),
      /* "set" (x,y) spatial elements of a VoxelKey */
      (k, sk) => VoxelKey(sk.col, sk.row, k.layer)
    )
  }

  implicit val depthComponent: Component[VoxelKey, DepthKey] = {
    Component[VoxelKey, DepthKey](
      k => DepthKey(k.layer),
      (k, dk) => VoxelKey(k.col, k.row, dk.depth)
    )
  }
}
