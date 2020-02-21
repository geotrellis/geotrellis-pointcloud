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

package geotrellis.pointcloud.tiling

import geotrellis.layer._
import geotrellis.pointcloud.raster.GridBounds3D
import geotrellis.pointcloud.vector.Extent3D
import geotrellis.util._
import geotrellis.vector.Extent

import org.locationtech.jts.{geom => jts}

/**
 * Convert spatial locations to VoxelKeys.
 *
 * Fundamental to Geotrellis is the notion of a layout.  This is a subdivision
 * of space into blocks, each with a unique key associated.  These keys are
 * reckoned relative to a region of space, given by an extent.  This extent is
 * chopped into a uniform grid of a given number of columns and rows.  Tiling of
 * the z-dimension is relative to a datum (usually zero), and a given layer
 * thickness.  Depth keys (the third component of VoxelKeys) have positive and
 * negative values, with non-negative values corresponding to layers above the
 * datum.  Locations with a z-value on the datum are included in the 0th layer—
 * that is, the ith depth key covers the half-open range
 *       [datum + i * layerThickness, datum + (i + 1) * layerThickness).
 */
class MapKeyTransform3D(override val extent: Extent,
                        override val layoutCols: Int,
                        override val layoutRows: Int,
                        val layerThickness: Double,
                        val datum: Double = 0.0) extends MapKeyTransform(extent, layoutCols, layoutRows) {

  private def zToLayer(z: Double): Int = {
    if (z.isNaN)
      throw new IllegalArgumentException("z-coordinate is not set!  Found NaN where value was expected.")

    math.floor((z - datum) / layerThickness).toInt
  }

  private def layerToZRange(layer: Int): (Double, Double) =
    (datum + layer * layerThickness, datum + (layer + 1) * layerThickness)

  def extentToBounds(otherExtent: Extent3D): GridBounds3D = {
    val gridBounds = extentToBounds(otherExtent.toExtent)
    val layerMin = zToLayer(otherExtent.zmin)
    val layerMax = zToLayer(otherExtent.zmax)

    GridBounds3D(gridBounds, layerMin, layerMax)
  }

  def apply(otherExtent: Extent3D): GridBounds3D = extentToBounds(otherExtent)

  def boundsToExtent(gridBounds: GridBounds3D): Extent3D = {
    val e1 = apply(gridBounds.colMin, gridBounds.rowMin, gridBounds.layerMin)
    val e2 = apply(gridBounds.colMax, gridBounds.rowMax, gridBounds.layerMax)
    e1.expandToInclude(e2)
  }

  def apply(gridBounds: GridBounds3D): Extent3D = boundsToExtent(gridBounds)

  /** Fetch the [[SpatialKey]] that corresponds to some coordinates in some CRS on the Earth. */
  def coordinateToKey(c: jts.Coordinate): VoxelKey = pointToKey(c.getX, c.getY, c.getZ)

  /** Fetch the [[SpatialKey]] that corresponds to some coordinates in some CRS on the Earth. */
  def pointToKey(x: Double, y: Double, z: Double): VoxelKey = {
    val SpatialKey(col, row) = pointToKey(x, y)

    val tlayer = zToLayer(z)

    VoxelKey(col, row, tlayer.floor.toInt)
  }

  def apply(x: Double, y: Double, z: Double): VoxelKey = pointToKey(x, y, z)

  def apply(c: jts.Coordinate): VoxelKey = pointToKey(c.getX, c.getY, c.getZ)

  /** Get the [[Extent]] corresponding to a [[SpatialKey]] in some zoom level. */
  def keyToExtent(key: SpatialKey, dk: DepthKey): Extent3D = keyToExtent(key.col, key.row, dk.depth)

  /** Get the [[Extent]] corresponding to a [[SpatialComponent]] of K in some zoom level. */
  def keyToExtent[K: SpatialComponent : DepthComponent](key: K): Extent3D = keyToExtent(key.getComponent[SpatialKey], key.getComponent[DepthKey])

  def apply(key: VoxelKey): Extent3D = keyToExtent(key)

  /** 'col', 'row', and 'layer' correspond to a [[VoxelKey]] column, row, and layer in some grid. */
  def keyToExtent(col: Int, row: Int, layer: Int): Extent3D = {
    val (zmin, zmax) = layerToZRange(layer)
    val Extent(xmin, ymin, xmax, ymax) = keyToExtent(col, row)
    Extent3D(xmin, ymin, zmin, xmax, ymax, zmax)
  }

  def apply(col: Int, row: Int, layer: Int): Extent3D = keyToExtent(col, row, layer)
}
