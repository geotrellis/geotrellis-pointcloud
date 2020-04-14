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

package geotrellis.pointcloud.raster.ept

import geotrellis.pointcloud.raster.rasterize.triangles.PDALTrianglesRasterizer
import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.OverviewStrategy
import geotrellis.vector._

import cats.syntax.option._
import _root_.io.circe.syntax._
import _root_.io.pdal.pipeline._
import org.log4s._

import scala.collection.JavaConverters._

case class TINRasterSource(
  path: EPTPath,
  resampleTarget: ResampleTarget = DefaultTarget,
  overviewStrategy: OverviewStrategy = OverviewStrategy.DEFAULT,
  sourceMetadata: Option[EPTMetadata] = None,
  threads: Option[Int] = None,
  targetCellType: Option[TargetCellType] = None
) extends RasterSource {
  @transient private[this] lazy val logger = getLogger

  lazy val metadata: EPTMetadata = sourceMetadata.getOrElse(EPTMetadata(path.value))

  def attributes: Map[String, String] = metadata.attributes
  def attributesForBand(band: Int): Map[String, String] = metadata.attributesForBand(band)
  def bandCount: Int = metadata.bandCount
  def cellType: CellType = metadata.cellType
  def crs: CRS = metadata.crs
  def gridExtent: GridExtent[Long] = resampleTarget(metadata.gridExtent)
  def name: SourceName = metadata.name
  def resolutions: List[CellSize] = metadata.resolutions

  def reprojection(targetCRS: CRS, resampleTarget: ResampleTarget, method: ResampleMethod, strategy: OverviewStrategy): TINReprojectRasterSource =
    TINReprojectRasterSource(path.value, targetCRS, resampleTarget, strategy, sourceMetadata = metadata.some, threads = threads, method, targetCellType = targetCellType)

  def resample(resampleTarget: ResampleTarget, method: ResampleMethod, strategy: OverviewStrategy): TINResampleRasterSource =
    TINResampleRasterSource(path.value, resampleTarget, strategy, metadata.some, threads, method, targetCellType)

  def read(bounds: GridBounds[Long], bands: Seq[Int]): Option[Raster[MultibandTile]] = {
    val targetRegion = gridExtent.extentFor(bounds, clamp = false)
    val Extent(exmin, eymin, exmax, eymax) = targetRegion.extent

    val res = OverviewStrategy.selectOverview(resolutions, gridExtent.cellSize, overviewStrategy)

    logger.debug(s"[TINRasterSource] Rendering TIN for ${RasterExtent(targetRegion, bounds.width.toInt, bounds.height.toInt)} with EPT resolution ${resolutions(res)} and strategy $overviewStrategy")

    val expression = ReadEpt(
      filename   = path.value,
      resolution = resolutions(res).resolution.some,
      bounds     = s"([$exmin, $eymin], [$exmax, $eymax])".some,
      threads    = threads
    ) ~ FilterDelaunay()

    logger.debug(expression.asJson.spaces4)

    val pipeline = expression toPipeline

    try {
      if(pipeline.validate()) {
        pipeline.execute

        val pointViews = pipeline.getPointViews().asScala.toList
        assert(pointViews.length == 1, "Triangulation pipeline should have single resulting point view")

        pointViews.headOption.map { pv =>
          try {
            PDALTrianglesRasterizer
              .native(pv, RasterExtent(targetRegion, bounds.width.toInt, bounds.height.toInt))
              .mapTile(MultibandTile(_))
          } finally pv.close()
        }
      } else None
    } finally pipeline.close()
  }

  def read(extent: Extent, bands: Seq[Int]): Option[Raster[MultibandTile]] = {
    val bounds = gridExtent.gridBoundsFor(extent.buffer(- cellSize.width / 2, - cellSize.height / 2), clamp = false)
    read(bounds, bands)
  }

  def convert(targetCellType: TargetCellType): RasterSource =
    throw new UnsupportedOperationException("DEM height fields may only be of floating point type")
}
