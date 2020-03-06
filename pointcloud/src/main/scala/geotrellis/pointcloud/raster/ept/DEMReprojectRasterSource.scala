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
import geotrellis.raster.reproject.{RasterRegionReproject, Reproject, ReprojectRasterExtent}
import geotrellis.vector._

import cats.syntax.option._
import _root_.io.circe.syntax._
import _root_.io.pdal._
import _root_.io.pdal.pipeline._
import geotrellis.raster.resample.NearestNeighbor
import org.log4s._

import scala.collection.JavaConverters._

/** TODO: replace it with io.pdal.pipeline.FilterReproject */
case class DEMReprojectRasterSource(
  eptSource: String,
  crs: CRS,
  resampleTarget: ResampleTarget = DefaultTarget,
  sourceMetadata: Option[EPTMetadata] = None,
  threads: Option[Int] = None,
  resampleMethod: ResampleMethod = NearestNeighbor,
  errorThreshold: Double = 0.125,
  targetCellType: Option[TargetCellType] = None
) extends RasterSource {
  @transient private[this] lazy val logger = getLogger

  lazy val metadata: EPTMetadata = sourceMetadata.getOrElse(EPTMetadata(eptSource))

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

  def reprojection(targetCRS: CRS, resampleTarget: ResampleTarget, method: ResampleMethod, strategy: OverviewStrategy): DEMReprojectRasterSource =
    DEMReprojectRasterSource(eptSource, targetCRS, resampleTarget, sourceMetadata = metadata.some, threads, method, errorThreshold, targetCellType)

  def resample(resampleTarget: ResampleTarget, method: ResampleMethod, strategy: OverviewStrategy): DEMReprojectRasterSource =
    DEMReprojectRasterSource(eptSource, crs, resampleTarget, sourceMetadata = metadata.some, threads, method, errorThreshold, targetCellType)

  def read(bounds: GridBounds[Long], bands: Seq[Int]): Option[Raster[MultibandTile]] = {
    bounds.intersection(dimensions).flatMap { targetPixelBounds =>
      val targetRegion = RasterExtent(
        extent = gridExtent.extentFor(targetPixelBounds, clamp = true),
        cols = targetPixelBounds.width.toInt,
        rows = targetPixelBounds.height.toInt
      )

      val sourceRegion = ReprojectRasterExtent(
        targetRegion,
        Proj4Transform(crs, baseCRS),
        Reproject.Options.DEFAULT
      )

      val bnds @ Extent(exmin, eymin, exmax, eymax) = sourceRegion.extent

      val expression = ReadEpt(
        filename   = eptSource,
        resolution = sourceRegion.cellSize.resolution.some,
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

          val pv = pointViews.head
          val pc = pv.getPointCloud(DimType.X, DimType.Y, DimType.Z)
          val tris = pv.getTriangularMesh()

          val w = sourceRegion.cellwidth
          val h = sourceRegion.cellheight
          val cols = sourceRegion.cols
          val rows = sourceRegion.rows
          val tile = DoubleArrayTile.empty(cols.toInt, rows.toInt)

          // TODO: remove from the RasterSource
          def rasterizeTriangle(tri: Triangle): Unit = {
            val Triangle(a, b, c) = tri

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

          tris.asArray.foreach(rasterizeTriangle)
          val sourceRaster = Raster(MultibandTile(tile), sourceRegion.extent)

          val rr = implicitly[RasterRegionReproject[MultibandTile]]
          val result = rr.regionReproject(
            sourceRaster,
            baseCRS,
            crs,
            targetRegion,
            targetRegion.extent.toPolygon,
            resampleMethod,
            errorThreshold
          )

          convertRaster(result).some
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
