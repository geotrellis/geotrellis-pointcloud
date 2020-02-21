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
import geotrellis.raster.{CellSize, GeoAttrsError}

class GridExtent3D(val extent: Extent3D, val cellwidth: Double, val cellheight: Double, val celldepth: Double) extends Serializable {
  def this(extent: Extent3D, voxelSize: VoxelSize) =
    this(extent, voxelSize.width, voxelSize.height, voxelSize.depth)

  def toRasterExtent3D: RasterExtent3D = {
    val targetCols = math.max(1L, math.round(extent.width / cellwidth))
    val targetRows = math.max(1L, math.round(extent.height / cellheight))
    val targetLayers = math.max(1L, math.round(extent.depth / celldepth))
    if(targetCols > Int.MaxValue) {
      throw new GeoAttrsError(s"Cannot convert GridExtent into a RasterExtent: number of columns exceeds maximum integer value ($targetCols > ${Int.MaxValue})")
    }
    if(targetRows > Int.MaxValue) {
      throw new GeoAttrsError(s"Cannot convert GridExtent into a RasterExtent: number of rows exceeds maximum integer value ($targetRows > ${Int.MaxValue})")
    }
    if(targetLayers > Int.MaxValue) {
      throw new GeoAttrsError(s"Cannot convert GridExtent into a RasterExtent: number of layers exceeds maximum integer value ($targetLayers > ${Int.MaxValue})")
    }

    RasterExtent3D(extent, cellwidth, cellheight, celldepth, targetCols.toInt, targetRows.toInt, targetLayers.toInt)
  }
}

object GridExtent3D {
  def apply(extent: Extent3D, voxelSize: VoxelSize) =
    new GridExtent3D(extent, voxelSize.width, voxelSize.height, voxelSize.depth)

  def apply(extent: Extent3D, cellSize: CellSize, zResolution: Double) =
    new GridExtent3D(extent, cellSize.width, cellSize.height, zResolution)
}
