package geotrellis.pointcloud.raster

import geotrellis.pointcloud.vector.Extent3D
import geotrellis.raster.{GeoAttrsError, RasterExtent}
import org.locationtech.jts.{geom => jts}

case class RasterExtent3D(
  override val extent: Extent3D,
  override val cellwidth: Double,
  override val cellheight: Double,
  override val celldepth: Double,
  cols: Int,
  rows: Int,
  layers: Int
) extends GridExtent3D(extent, cellwidth, cellheight, celldepth) with VolumetricGrid {
  if (cols <= 0) throw GeoAttrsError(s"invalid cols: $cols")
  if (rows <= 0) throw GeoAttrsError(s"invalid rows: $rows")
  if (layers <= 0) throw GeoAttrsError(s"invalid layers: $layers")

  /**
    * Convert map coordinates (x, y, z) to grid coordinates (col, row, layer).
    */
  final def mapToGrid(x: Double, y: Double, z: Double): (Int, Int, Int) =
    (mapXToGrid(x), mapYToGrid(y), mapZToGrid(z))

  /**
    * Convert map coordinate x to grid coordinate column.
    */
  final def mapXToGrid(x: Double): Int = math.floor(mapXToGridDouble(x)).toInt

  /**
    * Convert map coordinate x to grid coordinate column.
    */
  final def mapXToGridDouble(x: Double): Double = (x - extent.xmin) / cellwidth

  /**
    * Convert map coordinate y to grid coordinate row.
    */
  final def mapYToGrid(y: Double): Int = math.floor(mapYToGridDouble(y)).toInt

  /**
    * Convert map coordinate y to grid coordinate row.
    */
  final def mapYToGridDouble(y: Double): Double = (extent.ymax - y) / cellheight

  /**
    * Convert map coordinate y to grid coordinate row.
    */
  final def mapZToGrid(z: Double): Int = math.floor(mapZToGridDouble(z)).toInt

  /**
    * Convert map coordinate y to grid coordinate row.
    */
  final def mapZToGridDouble(z: Double): Double = (extent.zmax - z) / celldepth

  /**
    * Convert map coordinate tuple (x, y, z) to grid coordinates (col, row, layer).
    */
  final def mapToGrid(mapCoord: (Double, Double, Double)): (Int, Int, Int) = {
    val (x, y, z) = mapCoord
    mapToGrid(x, y, z)
  }

  /**
   * Convert a point to grid coordinates (col, row).
   */
  final def mapToGrid(c: jts.Coordinate): (Int, Int, Int) =
    mapToGrid(c.getX, c.getY, c.getZ)

  /**
    * The map coordinate of a grid cell is the center point.
    */
  final def gridToMap(col: Int, row: Int, layer: Int): (Double, Double, Double) = {
    (gridColToMap(col), gridRowToMap(row), gridLayerToMap(layer))
  }

  /**
    * For a given column, find the corresponding x-coordinate in the
    * grid of the present [[RasterExtent3D]].
    */
  final def gridColToMap(col: Int): Double = {
    col * cellwidth + extent.xmin + (cellwidth / 2)
  }

  /**
    * For a given row, find the corresponding y-coordinate in the grid
    * of the present [[RasterExtent3D]].
    */
  final def gridRowToMap(row: Int): Double = {
    extent.ymax - (row * cellheight) - (cellheight / 2)
  }

  /**
    * For a given layer, find the corresponding z-coordinate in the grid
    * of the present [[RasterExtent3D]].
    */
  final def gridLayerToMap(layer: Int): Double = {
    extent.zmax - (layer * celldepth) - (celldepth / 2)
  }

  /**
    * Gets the GridBounds aligned with this RasterExtent that is the
    * smallest subgrid of containing all points within the extent. The
    * extent is considered inclusive on it's north and west borders,
    * exclusive on it's east and south borders.  See [[RasterExtent]]
    * for a discussion of grid and extent boundary concepts.
    *
    * The 'clamp' flag determines whether or not to clamp the
    * GridBounds to the RasterExtent; defaults to true. If false,
    * GridBounds can contain negative values, or values outside of
    * this RasterExtent's boundaries.
    *
    * @param     subExtent      The extent to get the grid bounds for
    * @param     clamp          A boolean
    */
  def gridBoundsFor(subExtent: Extent3D, clamp: Boolean = true): GridBounds3D = {
    // West and North boundaries are a simple mapToGrid call.
    val (colMin, rowMin, layerMin) = mapToGrid(subExtent.xmin, subExtent.ymax, subExtent.zmax)

    // If South East corner is on grid border lines, we want to still only include
    // what is to the West and\or North of the point. However if the border point
    // is not directly on a grid division, include the whole row and/or column that
    // contains the point.
    val colMax = {
      val colMaxDouble = mapXToGridDouble(subExtent.xmax)
      if(math.abs(colMaxDouble - math.floor(colMaxDouble)) < RasterExtent.epsilon) colMaxDouble.toInt - 1
      else colMaxDouble.toInt
    }

    val rowMax = {
      val rowMaxDouble = mapYToGridDouble(subExtent.ymin)
      if(math.abs(rowMaxDouble - math.floor(rowMaxDouble)) < RasterExtent.epsilon) rowMaxDouble.toInt - 1
      else rowMaxDouble.toInt
    }

    val layerMax = {
      val layerMaxDouble = mapZToGridDouble(subExtent.zmin)
      if(math.abs(layerMaxDouble - math.floor(layerMaxDouble)) < RasterExtent.epsilon) layerMaxDouble.toInt - 1
      else layerMaxDouble.toInt
    }

    if(clamp) {
      GridBounds3D(math.min(math.max(colMin, 0), cols - 1),
                   math.min(math.max(rowMin, 0), rows - 1),
                   math.min(math.max(layerMin, 0), layers - 1),
                   math.min(math.max(colMax, 0), cols - 1),
                   math.min(math.max(rowMax, 0), rows - 1),
                   math.min(math.max(layerMax, 0), layers - 1))
    } else {
      GridBounds3D(colMin, rowMin, layerMin, colMax, rowMax, layerMax)
    }
  }


}
