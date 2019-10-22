package geotrellis.pointcloud.raster

import geotrellis.raster.GridBounds

case class GridBounds3D(colMin: Int, rowMin: Int, layerMin: Int, colMax: Int, rowMax: Int, layerMax: Int) {
  def width = colMax - colMin + 1
  def height = rowMax - rowMin + 1
  def depth = layerMax - layerMin + 1

  def size: Long = width.toLong * height.toLong * depth.toLong
}

object GridBounds3D {
  def apply(gridBounds: GridBounds, layerMin: Int, layerMax: Int): GridBounds3D =
    GridBounds3D(gridBounds.colMin, gridBounds.rowMin, layerMin, gridBounds.colMax, gridBounds.rowMax, layerMax)
}
