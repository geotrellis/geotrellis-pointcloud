package geotrellis.pointcloud.tiling

import geotrellis.pointcloud.raster.{GridExtent3D, VoxelSize}
import geotrellis.pointcloud.vector.Extent3D
import geotrellis.raster.TileLayout
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.vector.Extent

case class LayoutDefinition3D(extent: Extent,
                              tileLayout: TileLayout,
                              cellDepth: Double,
                              tileLayers: Int,
                              datum: Double = 0.0) {

  def tileCols = tileLayout.tileCols
  def tileRows = tileLayout.tileRows
  def layoutCols = tileLayout.layoutCols
  def layoutRows = tileLayout.layoutRows
  val layerThickness: Double = tileLayers * cellDepth

  lazy val mapTransform = new MapKeyTransform3D(extent, tileLayout.layoutCols, tileLayout.layoutRows, layerThickness, datum)

  def layout2D = LayoutDefinition(extent, tileLayout)
}

object LayoutDefinition3D {
  def apply(layoutDefinition: LayoutDefinition, layerThickness: Double, tileLayers: Int): LayoutDefinition3D =
    LayoutDefinition3D(layoutDefinition.extent, layoutDefinition.tileLayout, layerThickness, tileLayers)

  def apply(layoutDefinition: LayoutDefinition, layerThickness: Double, tileLayers: Int, datum: Double): LayoutDefinition3D =
    LayoutDefinition3D(layoutDefinition.extent, layoutDefinition.tileLayout, layerThickness, tileLayers, datum)
}
