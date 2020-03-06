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
import _root_.io.circe.syntax._
import _root_.io.pdal._
import _root_.io.pdal.pipeline._
import org.log4s._

import scala.collection.JavaConverters._

case class DEMRasterSource(
  eptSource: String,
  resampleTarget: ResampleTarget = DefaultTarget,
  destCRS: Option[CRS] = None,
  targetCellType: Option[TargetCellType] = None,
  sourceMetadata: Option[EPTMetadata] = None,
  threads: Option[Int] = None
) extends RasterSource {
  @transient private[this] lazy val logger = getLogger

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
    DEMRasterSource(eptSource, resampleTarget, targetCRS.some, sourceMetadata = metadata.some, threads = threads)

  def resample(resampleTarget: ResampleTarget, method: ResampleMethod, strategy: OverviewStrategy): RasterSource =
    DEMRasterSource(eptSource, resampleTarget, destCRS, sourceMetadata = metadata.some, threads = threads)

  def read(bounds: GridBounds[Long], bands: Seq[Int]): Option[Raster[MultibandTile]] = {
    val targetRegion = gridExtent.extentFor(bounds, clamp = false)
    val srcBounds = ReprojectRasterExtent(
      GridExtent(targetRegion, bounds.width, bounds.height),
      Proj4Transform(crs, metadata.crs),
      Reproject.Options.DEFAULT
    )
    val bnds @ Extent(exmin, eymin, exmax, eymax) = srcBounds.extent

    val expression = ReadEpt(
      filename   = eptSource,
      resolution = gridExtent.cellSize.resolution.some,
      bounds     = s"([${bnds.xmin}, ${bnds.ymin}], [${bnds.xmax}, ${bnds.ymax}])".some,
      threads    = threads
    ) ~ FilterDelaunay()

    logger.debug(expression.asJson.spaces4)

    val pipeline = expression toPipeline

    try {
      if(pipeline.validate()) {
        pipeline.execute

        val pointViews = pipeline.getPointViews().asScala.toList
        assert(pointViews.length == 1, "Triangulation pipeline should have single resulting point view")

        val pc = pointViews(0).getPointCloud(DimType.X, DimType.Y, DimType.Z)
        val tris = pointViews(0).getTriangularMesh()

        val w = gridExtent.cellwidth
        val h = gridExtent.cellheight
        val cols = bounds.width
        val rows = bounds.height
        val tile = DoubleArrayTile.empty(cols.toInt, rows.toInt)

        def rasterizeTriangle(tri: Triangle): Unit = {
          val (a,b,c) = (tri.a, tri.b, tri.c)

          val v1x = pc.getX(a)
          val v1y = pc.getY(a)
          val v1z = pc.getZ(a)
          val s1x = pc.getX(c)
          val s1y = pc.getY(c)

          val v2x = pc.getX(b)
          val v2y = pc.getY(b)
          val v2z = pc.getZ(b)
          val s2x = pc.getX(a)
          val s2y = pc.getY(a)

          val v3x = pc.getX(c)
          val v3y = pc.getY(c)
          val v3z = pc.getZ(c)
          val s3x = pc.getX(b)
          val s3y = pc.getY(b)

          val determinant =
          (v2y - v3y) * (v1x - v3x) + (v3x - v2x) * (v1y - v3y)

          val ymin =
            math.min(v1y, math.min(v2y, v3y))
          val ymax =
              math.max(v1y, math.max(v2y, v3y))

          val scanrow0 = math.max(math.ceil((ymin - eymin) / h - 0.5), 0)
          var scany = eymin + scanrow0 * h + h / 2
          while (scany < eymax && scany < ymax) {
            // get x at y for edge
            var xmin = Double.MinValue
            var xmax = Double.MaxValue

            if(s1y != v1y) {
              val t = (scany - v1y) / (s1y - v1y)
              val xAtY1 = v1x + t * (s1x - v1x)

              if(v1y < s1y) {
                // Lefty
                if(xmin < xAtY1) { xmin = xAtY1 }
              } else {
                // Righty
                if(xAtY1 < xmax) { xmax = xAtY1 }
              }
            }

            if(s2y != v2y) {
              val t = (scany - v2y) / (s2y - v2y)
              val xAtY2 = v2x + t * (s2x - v2x)

              if(v2y < s2y) {
                // Lefty
                if(xmin < xAtY2) { xmin = xAtY2 }
              } else {
                // Righty
                if(xAtY2 < xmax) { xmax = xAtY2 }
              }
            }

            if(s3y != v3y) {
              val t = (scany - v3y) / (s3y - v3y)
              val xAtY3 = v3x + t * (s3x - v3x)

              if(v3y < s3y) {
                // Lefty
                if(xmin < xAtY3) { xmin = xAtY3 }
              } else {
                // Righty
                if(xAtY3 < xmax) { xmax = xAtY3 }
              }
            }

            val scancol0 = math.max(math.ceil((xmin - exmin) / w - 0.5), 0)
            var scanx = exmin + scancol0 * w + w / 2
            while (scanx < exmax && scanx < xmax) {
              val col = ((scanx - exmin) / w).toInt
              val row = ((eymax - scany) / h).toInt
              if(0 <= col && col < cols &&
                 0 <= row && row < rows) {

                val z = {

                  val lambda1 =
                  ((v2y - v3y) * (scanx - v3x) + (v3x - v2x) * (scany - v3y)) / determinant

                  val lambda2 =
                  ((v3y - v1y) * (scanx - v3x) + (v1x - v3x) * (scany - v3y)) / determinant

                  val lambda3 = 1.0 - lambda1 - lambda2

                  lambda1 * v1z + lambda2 * v2z + lambda3 * v3z
                }

                tile.setDouble(col, row, z)
              }

              scanx += w
            }

            scany += h
          }
        }

        tris.asScala.foreach(rasterizeTriangle(_))
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
