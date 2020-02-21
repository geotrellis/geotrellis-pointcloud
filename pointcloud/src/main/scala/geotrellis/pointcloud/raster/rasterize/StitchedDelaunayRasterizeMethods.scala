/*
 * Copyright 2017 Azavea
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

package geotrellis.pointcloud.raster.rasterize

import geotrellis.raster.triangulation.DelaunayRasterizer
import geotrellis.raster.{ArrayTile, CellType, DoubleConstantNoDataCellType, RasterExtent}
import geotrellis.util.MethodExtensions
import geotrellis.vector.triangulation.{DelaunayTriangulation, StitchedDelaunay}

trait StitchedDelaunayRasterizeMethods extends MethodExtensions[StitchedDelaunay] {
  def rasterize(re: RasterExtent, cellType: CellType = DoubleConstantNoDataCellType)(center: DelaunayTriangulation) = {
    val tile = ArrayTile.empty(cellType, re.cols, re.rows)
    DelaunayRasterizer.rasterizeDelaunayTriangulation(center, re, tile)
    DelaunayRasterizer.rasterize(
      tile,
      re,
      self.fillTriangles,
      self.halfEdgeTable,
      self.pointSet
    )
    tile
  }
}
