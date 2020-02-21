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

package geotrellis.pointcloud.raster

import geotrellis.raster.GridBounds
import spire.math._
import spire.implicits._

case class GridBounds3D[@specialized(Int, Long) N: Integral](colMin: N, rowMin: N, layerMin: N, colMax: N, rowMax: N, layerMax: N) {
  def width: N = colMax - colMin + 1
  def height: N = rowMax - rowMin + 1
  def depth: N = layerMax - layerMin + 1

  def size: N = width * height * depth
}

object GridBounds3D {
  def apply[@specialized(Int, Long) N: Integral](gridBounds: GridBounds[N], layerMin: N, layerMax: N): GridBounds3D[N] =
    GridBounds3D(gridBounds.colMin, gridBounds.rowMin, layerMin, gridBounds.colMax, gridBounds.rowMax, layerMax)
}
