package geotrellis.pointcloud.raster

trait VolumetricGrid extends Serializable {
  def cols: Int
  def rows: Int
  def layers: Int

  /**
    * The size of the grid, e.g. cols * rows * layers.
    */
  def size = cols * rows * layers
  def dimensions: (Int, Int, Int) = (cols, rows, layers)
  def gridBounds: GridBounds3D = GridBounds3D(0, 0, 0, cols - 1, rows - 1, layers - 1)
}
