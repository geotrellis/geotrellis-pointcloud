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

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.OverviewStrategy
import geotrellis.raster.reproject.{Reproject, ReprojectRasterExtent}
import geotrellis.raster.triangulation._
import geotrellis.vector._
import geotrellis.vector.triangulation._

import cats.syntax.option._
import org.locationtech.jts.geom.Coordinate
import spire.syntax.cfor._
import _root_.io.pdal._
import _root_.io.pdal.pipeline._

import scala.collection.JavaConverters._

case class DEMRasterSource(
  eptSource: String,
  resampleTarget: ResampleTarget = DefaultTarget,
  destCRS: Option[CRS] = None,
  targetCellType: Option[TargetCellType] = None,
  sourceMetadata: Option[EPTMetadata] = None
) extends RasterSource {
  lazy val metadata: EPTMetadata = sourceMetadata.getOrElse(EPTMetadata(eptSource))

  def attributes: Map[String, String] = metadata.attributes
  def attributesForBand(band: Int): Map[String, String] = metadata.attributesForBand(band)
  def bandCount: Int = metadata.bandCount
  def cellType: CellType = metadata.cellType
  def crs: CRS = destCRS.getOrElse(metadata.crs)
  lazy val gridExtent: GridExtent[Long] = {
    lazy val reprojectedRasterExtent =
      ReprojectRasterExtent(
        metadata.gridExtent,
        Transform(metadata.crs, crs),
        Reproject.Options.DEFAULT
      )

    resampleTarget match {
      case targetRegion: TargetRegion         => targetRegion.region.toGridType[Long]
      case targetAlignment: TargetAlignment   => targetAlignment(metadata.gridExtent)
      case targetDimensions: TargetDimensions => targetDimensions(metadata.gridExtent)
      case targetCellSize: TargetCellSize     => targetCellSize(metadata.gridExtent)
      case _                                  => reprojectedRasterExtent
    }
  }

  def name: SourceName = metadata.name
  def resolutions: List[CellSize] = metadata.resolutions

  def reprojection(targetCRS: CRS, resampleTarget: ResampleTarget, method: ResampleMethod, strategy: OverviewStrategy): DEMRasterSource =
    DEMRasterSource(eptSource, resampleTarget, targetCRS.some, sourceMetadata = metadata.some)

  def resample(resampleTarget: ResampleTarget, method: ResampleMethod, strategy: OverviewStrategy): RasterSource =
    DEMRasterSource(eptSource, resampleTarget, destCRS, sourceMetadata = metadata.some)

  def read(bounds: GridBounds[Long], bands: Seq[Int]): Option[Raster[MultibandTile]] = {
    val targetRegion = gridExtent.extentFor(bounds, clamp = false)
    val srcBounds = ReprojectRasterExtent(
      GridExtent(targetRegion, bounds.width, bounds.height),
      Proj4Transform(crs, metadata.crs),
      Reproject.Options.DEFAULT
    )
    val bnds = srcBounds.extent

    val pipeline = ReadEpt(
      filename   = eptSource,
      resolution = gridExtent.cellSize.resolution.some,
      bounds     = s"([${bnds.xmin}, ${bnds.ymin}], [${bnds.xmax}, ${bnds.ymax}])".some
    ) toPipeline

    try {
      if(pipeline.validate()) {
        pipeline.execute

        val pointViews = pipeline.getPointViews().asScala.toList
        val viewSizes = pointViews.map(_.length)
        val (nCoords, starts) = viewSizes.foldLeft(0 -> List.empty[Int]) { case ((acc, cum), v) => (v + acc, cum :+ acc) }
        val coords = Array.ofDim[Coordinate](nCoords)
        viewSizes.zip(starts).zip(pointViews).foreach { case ((n, z), pv) =>
          val pc = pv.getPointCloud(DimType.X, DimType.Y, DimType.Z)
          cfor(0)(_ < n, _ + 1) { i => coords(z + i) = pc.getCoordinate(i) }
        }

        val dt = DelaunayTriangulation(coords)
        val tile = DelaunayRasterizer.rasterizeDelaunayTriangulation(dt, srcBounds.toRasterExtent)
        Raster(MultibandTile(tile), targetRegion).some
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
