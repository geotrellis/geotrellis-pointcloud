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

case class GridBounds3D(colMin: Int, rowMin: Int, layerMin: Int, colMax: Int, rowMax: Int, layerMax: Int) {
  def width = colMax - colMin + 1
  def height = rowMax - rowMin + 1
  def depth = layerMax - layerMin + 1

  def size: Long = width.toLong * height.toLong * depth.toLong
}

object GridBounds3D {
  def apply(gridBounds: GridBounds[Int], layerMin: Int, layerMax: Int): GridBounds3D =
    GridBounds3D(gridBounds.colMin, gridBounds.rowMin, layerMin, gridBounds.colMax, gridBounds.rowMax, layerMax)
}
