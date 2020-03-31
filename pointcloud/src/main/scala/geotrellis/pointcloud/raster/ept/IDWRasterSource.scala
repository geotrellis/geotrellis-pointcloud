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

import geotrellis.pointcloud.raster.rasterize.points.IDWRasterizer
import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.interpolation._
import geotrellis.raster.io.geotiff.OverviewStrategy
import geotrellis.vector._

import cats.syntax.option._
import _root_.io.circe.syntax._
import _root_.io.pdal.pipeline._
import org.log4s._
import spire.syntax.cfor._

import scala.collection.JavaConverters._

/**
  * [[DEMRasterSource]] doesn't use [[OverviewStrategy]].
  * At this point, it relies on the EPTReader logic:
  * https://github.com/PDAL/PDAL/blob/2.1.0/io/EptReader.cpp#L293-L318
  */
case class IDWRasterSource(
  path: EPTPath,
  resampleTarget: ResampleTarget = DefaultTarget,
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
  def gridExtent: GridExtent[Long] = metadata.gridExtent
  def name: SourceName = metadata.name
  def resolutions: List[CellSize] = metadata.resolutions

  def reprojection(targetCRS: CRS, resampleTarget: ResampleTarget, method: ResampleMethod, strategy: OverviewStrategy): IDWReprojectRasterSource =
    IDWReprojectRasterSource(path.value, targetCRS, resampleTarget, sourceMetadata = metadata.some, threads = threads, method, targetCellType = targetCellType)

  def resample(resampleTarget: ResampleTarget, method: ResampleMethod, strategy: OverviewStrategy): IDWResampleRasterSource =
    IDWResampleRasterSource(path.value, resampleTarget, metadata.some, threads, method, targetCellType)

  def read(bounds: GridBounds[Long], bands: Seq[Int]): Option[Raster[MultibandTile]] = {
    val targetRegion = gridExtent.extentFor(bounds, clamp = false)
    val Extent(exmin, eymin, exmax, eymax) = targetRegion.extent

    val expression = ReadEpt(
      filename   = path.value,
      resolution = gridExtent.cellSize.resolution.some,
      bounds     = s"([$exmin, $eymin], [$exmax, $eymax])".some,
      threads    = threads
    )

    logger.debug(expression.asJson.spaces4)

    val pipeline = expression toPipeline

    try {
      if(pipeline.validate()) {
        pipeline.execute

        val pointViews = pipeline.getPointViews().asScala.toList
        assert(pointViews.length == 1, "Triangulation pipeline should have single resulting point view")

        pointViews.headOption.map { pv =>
          IDWRasterizer(
            pv,
            RasterExtent(
              targetRegion,
              bounds.width.toInt,
              bounds.height.toInt
            )
          )
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
