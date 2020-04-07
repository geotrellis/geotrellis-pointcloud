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

package geotrellis.pointcloud.raster.rasterize.points

import geotrellis.raster._
import geotrellis.raster.interpolation._
import geotrellis.vector.{Point, PointFeature}

import _root_.io.pdal.{DimType, PointView}
import spire.syntax.cfor._

object IDWRasterizer {
  def apply(pv: PointView, re: RasterExtent, radiusMultiplier: Double = 1.5, cellType: CellType = DoubleConstantNoDataCellType): Raster[Tile] = {
    val pc = pv.getPointCloud(Array(DimType.X, DimType.Y, DimType.Z))
    val features = Array.ofDim[PointFeature[Double]](pc.length)
    cfor(0)(_ < pc.length, _ + 1) { i =>
      features(i) = PointFeature[Double](Point(pc.getX(i), pc.getY(i)), pc.getZ(i))
    }

    val CellSize(w, h) = re.cellSize
    val radius = radiusMultiplier * math.sqrt(w * w + h * h)

    features
      .toTraversable
      .inverseDistanceWeighted(
        re,
        InverseDistanceWeighted.Options(
          radius,
          radius,
          0.0, // rotation
          3.0, // weighting power
          0.0, // smoothing factor
          re.cellSize.resolution / 2, //equal weight radius
          cellType
        ))
  }
}
