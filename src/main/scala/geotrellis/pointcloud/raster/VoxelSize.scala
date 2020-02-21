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

import geotrellis.pointcloud.vector.Extent3D
import geotrellis.raster.CellSize

/**
  * A case class containing the width and height of a cell.
  *
  * @param  width   The width of a cell
  * @param  height  The height of a cell
  */
case class VoxelSize(width: Double, height: Double, depth: Double) {
  def resolution: Double = math.pow(width*height*depth, 1.0/3.0)
}

/**
  * The companion object for the [[CellSize]] type.
  */
object VoxelSize {

  /**
    * Create a new [[VoxelSize]] from an extent, a number of columns,
    * a number of rows, and a number of layers.
    *
    * @param   extent  The extent, which provides an overall height and width
    * @param   cols    The number of columns
    * @param   rows    The number of rows
    * @param   layers  The number of layers
    * @return          The VoxelSize
    */
  def apply(extent: Extent3D, cols: Int, rows: Int, layers: Int): VoxelSize =
    VoxelSize(extent.width / cols, extent.height / rows, extent.depth / layers)

  /**
    * Create a new [[VoxelSize]] from an extent, a number of columns,
    * a number of rows, and a number of layers.
    *
    * @param   extent  The extent, which provides an overall height and width
    * @param   dims    The numbers of columns and rows as a tuple
    * @return          The CellSize
    */
  def apply(extent: Extent3D, dims: (Int, Int, Int)): VoxelSize = {
    val (cols, rows, layers) = dims
    apply(extent, cols, rows, layers)
  }

  def apply(cellSize: CellSize, zResolution: Double): VoxelSize =
    VoxelSize(cellSize.width, cellSize.height, zResolution)

  /**
    * Create a new [[CellSize]] from a string containing the width and
    * height separated by a comma.
    *
    * @param   s  The string
    * @return     The CellSize
    */
  def fromString(s:String) = {
    val Array(width, height, depth) = s.split(",").map(_.toDouble)
    VoxelSize(width, height, depth)
  }
}
