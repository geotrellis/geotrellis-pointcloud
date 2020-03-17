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
import geotrellis.raster.reproject.{RasterRegionReproject, Reproject, ReprojectRasterExtent}
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.vector._

import _root_.io.circe.syntax._
import _root_.io.pdal.pipeline._
import cats.syntax.option._
import org.log4s._

import scala.collection.JavaConverters._

case class JavaDEMReprojectRasterSource(
  path: EPTPath,
  crs: CRS,
  resampleTarget: ResampleTarget = DefaultTarget,
  sourceMetadata: Option[EPTMetadata] = None,
  threads: Option[Int] = None,
  resampleMethod: ResampleMethod = NearestNeighbor,
  errorThreshold: Double = 0.125,
  targetCellType: Option[TargetCellType] = None
) extends RasterSource {
  @transient private[this] lazy val logger = getLogger

  lazy val metadata: EPTMetadata = sourceMetadata.getOrElse(EPTMetadata(path.value))

  protected lazy val baseCRS: CRS = metadata.crs
  protected lazy val baseGridExtent: GridExtent[Long] = metadata.gridExtent

  // TODO: remove transient notation with Proj4 1.1 release
  @transient protected lazy val transform = Transform(baseCRS, crs)
  @transient protected lazy val backTransform = Transform(crs, baseCRS)

  def attributes: Map[String, String] = metadata.attributes
  def attributesForBand(band: Int): Map[String, String] = metadata.attributesForBand(band)
  def bandCount: Int = metadata.bandCount
  def cellType: CellType = metadata.cellType
  def name: SourceName = metadata.name
  def resolutions: List[CellSize] = metadata.resolutions

  lazy val gridExtent: GridExtent[Long] = {
    lazy val reprojectedRasterExtent =
      ReprojectRasterExtent(
        baseGridExtent,
        transform,
        Reproject.Options.DEFAULT.copy(method = resampleMethod, errorThreshold = errorThreshold)
      )

    resampleTarget match {
      case targetRegion: TargetRegion => targetRegion.region.toGridType[Long]
      case targetAlignment: TargetAlignment => targetAlignment(reprojectedRasterExtent)
      case targetDimensions: TargetDimensions => targetDimensions(reprojectedRasterExtent)
      case targetCellSize: TargetCellSize => targetCellSize(reprojectedRasterExtent)
      case _ => reprojectedRasterExtent
    }
  }

  def reprojection(targetCRS: CRS, resampleTarget: ResampleTarget, method: ResampleMethod, strategy: OverviewStrategy): JavaDEMReprojectRasterSource =
    JavaDEMReprojectRasterSource(path, targetCRS, resampleTarget, sourceMetadata = metadata.some, threads, method, errorThreshold, targetCellType)

  def resample(resampleTarget: ResampleTarget, method: ResampleMethod, strategy: OverviewStrategy): JavaDEMReprojectRasterSource =
    JavaDEMReprojectRasterSource(path, crs, resampleTarget, sourceMetadata = metadata.some, threads, method, errorThreshold, targetCellType)

  def read(bounds: GridBounds[Long], bands: Seq[Int]): Option[Raster[MultibandTile]] = {
    bounds.intersection(dimensions).flatMap { targetPixelBounds =>
      val targetRegion = RasterExtent(
        extent = gridExtent.extentFor(targetPixelBounds, clamp = true),
        cols = targetPixelBounds.width.toInt,
        rows = targetPixelBounds.height.toInt
      )

      /** Buffer the targetRegion to generate a buffered raster from a mesh to perform a more precise region reproject */
      val bufferedTargetRegion = RasterExtent(targetRegion.extent.buffer(2 * targetRegion.cellwidth, 2 * targetRegion.cellheight), targetRegion.cellSize)
      val bufferedSourceRegion = ReprojectRasterExtent(bufferedTargetRegion, backTransform, Reproject.Options.DEFAULT)

      val Extent(exmin, eymin, exmax, eymax) = bufferedSourceRegion.extent

      val expression = ReadEpt(
        filename   = path.value,
        resolution = bufferedSourceRegion.cellSize.resolution.some,
        bounds     = s"([$exmin, $eymin], [$exmax, $eymax])".some,
        threads    = threads
      ) ~ FilterReprojection(crs.toProj4String, baseCRS.toProj4String.some) ~ FilterDelaunay()

      logger.debug(expression.asJson.spaces4)

      val pipeline = expression toPipeline

      try {
        if(pipeline.validate()) {
          pipeline.execute

          val pointViews = pipeline.getPointViews().asScala.toList
          assert(pointViews.length == 1, "Triangulation pipeline should have single resulting point view")

          val pv = pointViews.head
          val sourceRaster =
            PDALTrianglesRasterizer
              .native(pv, targetRegion)
              .mapTile(MultibandTile(_))

          convertRaster(sourceRaster).some
        } else None
      } finally pipeline.close()
    }
  }

  def read(extent: Extent, bands: Seq[Int]): Option[Raster[MultibandTile]] = {
    val bounds = gridExtent.gridBoundsFor(extent.buffer(- cellSize.width / 2, - cellSize.height / 2), clamp = false)
    read(bounds, bands)
  }

  def convert(targetCellType: TargetCellType): RasterSource =
    throw new UnsupportedOperationException("DEM height fields may only be of floating point type")
}


