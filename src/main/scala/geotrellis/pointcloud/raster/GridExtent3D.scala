package geotrellis.pointcloud.raster

import geotrellis.pointcloud.vector.Extent3D
import geotrellis.raster.{CellSize, GeoAttrsError}

class GridExtent3D(val extent: Extent3D, val cellwidth: Double, val cellheight: Double, val celldepth: Double) extends Serializable {
  def this(extent: Extent3D, voxelSize: VoxelSize) =
    this(extent, voxelSize.width, voxelSize.height, voxelSize.depth)

  def toRasterExtent3D: RasterExtent3D = {
    val targetCols = math.max(1L, math.round(extent.width / cellwidth).toLong)
    val targetRows = math.max(1L, math.round(extent.height / cellheight).toLong)
    val targetLayers = math.max(1L, math.round(extent.depth / celldepth).toLong)
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
