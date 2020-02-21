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

package geotrellis.pointcloud.raster.rasterize.polygon

import geotrellis.raster._
import geotrellis.pointcloud.raster.rasterize.triangles._
import geotrellis.vector._
import geotrellis.vector.voronoi._

import org.locationtech.jts.{geom => jts}
import org.locationtech.jts.index.strtree.STRtree
import spire.syntax.cfor._

import scala.collection.JavaConverters._
import scala.collection.mutable

object Polygon3DRasterizer {
  private def polygonToEdges(poly: Polygon): Seq[LineString] = {

    val arrayBuffer = mutable.ArrayBuffer.empty[LineString]

    /** Find the outer ring's segments */
    val coords = poly.getExteriorRing.getCoordinates
    cfor(1)(_ < coords.length, _ + 1) { ci =>
      val coord1 = coords(ci - 1)
      val coord2 = coords(ci)
      val segment = LineString(Point(coord1.getX, coord1.getY), Point(coord2.getX, coord2.getY))

      arrayBuffer += segment
    }

    /** Find the segments for the holes */
    cfor(0)(_ < poly.getNumInteriorRing, _ + 1) { i =>
      val coords = poly.getInteriorRingN(i).getCoordinates
      cfor(1)(_ < coords.length, _ + 1) { ci =>
        val coord1 = coords(ci - 1)
        val coord2 = coords(ci)
        val segment = LineString(Point(coord1.getX, coord1.getY), Point(coord2.getX, coord2.getY))

        arrayBuffer += segment
      }
    }

    arrayBuffer
  }

  def rasterizePolygon(geom: jts.Polygon, tile: MutableArrayTile, re: RasterExtent): Unit = {
    val constraints = polygonToEdges(geom)
    val coordinates = geom.getCoordinates
    val points = coordinates.map({ coord => Point(coord.getX, coord.getY) })
    val dt = ConformingDelaunay(points, constraints)
    val triangles = dt.triangles.filter({ triangle => geom.contains(triangle) })
    val steinerPoints = dt.steinerPoints

    /**
      * Insert constraints into search tree.  It is assumed that all
      * Steiner points will be generated on constraints.
      */
    val rtree = new STRtree
    constraints.foreach({ line =>
      val a = line.points(0)
      val b = line.points(1)
      val xmin = math.min(a.x, b.x)
      val xmax = math.max(a.x, b.x)
      val ymin = math.min(a.y, b.y)
      val ymax = math.max(a.y, b.y)
      val envelope = new jts.Envelope(xmin, xmax, ymin, ymax)
      rtree.insert(envelope, line)
    })

    /** Build index map */
    val indexMap: Map[(Double, Double), Int] = (points ++ steinerPoints)
      .zipWithIndex
      .map({ case (point, index) =>
        (point.x, point.y) -> index })
      .toMap

    /** z-coordinates of original vertices */
    val zs1 = geom.getCoordinates.map(_.getZ)

    /** z-coordinates of Steiner vertices */
    val zs2 = steinerPoints.map({ point =>
      val envelope = new jts.Envelope(point.x, point.x, point.y, point.y)
      val lines = rtree.query(envelope).asScala; require(lines.nonEmpty)
      val line =
        lines
          .map({ line => line.asInstanceOf[LineString] })
          .map({ line => (line.distance(point), line) })
          .reduce({ (pair1, pair2) => if (pair1._1 <= pair2._1) pair1; else pair2 })
          ._2
      val a = line.points(0) // first endpoint
      val aIndex = indexMap.getOrElse((a.x, a.y), throw new Exception) // index of first endpoint
      val aValue = zs1(aIndex) // z value at first endpoint
      val b = line.points(1) // second endpoint
      val bIndex = indexMap.getOrElse((b.x, b.y), throw new Exception) // index of second endpoint
      val bValue = zs1(bIndex) // z value at second endpoint
      val dista = a.distance(point) // distance from Steiner point to first endpoint
      val distb = b.distance(point) // distance from Steiner point  to second endpoint
      val t = distb / (dista + distb) // the proportion of the interpolated value that should come from the first endpoint

      t*aValue + (1-t)*bValue }) // the interpolated value
      .toArray

    /** Build source array */
    val sourceArray: Array[Double] = zs1 ++ zs2

    TrianglesRasterizer(re, tile, sourceArray, triangles, indexMap)
  }

  def rasterize(geom: jts.Geometry, re: RasterExtent, cellType: CellType = DoubleConstantNoDataCellType): Raster[Tile] = {
    val tile = ArrayTile.empty(cellType, re.cols, re.rows)

    rasterize(tile, geom, re)

    Raster(tile, re.extent)
  }

  def rasterize(tile: MutableArrayTile, geom: jts.Geometry, re: RasterExtent): Unit = {
    val extentGeom = re.extent.toPolygon
    geom match {
      case p: jts.Polygon => if(p.intersects(extentGeom)) rasterizePolygon(p, tile, re)
      case mp: jts.MultiPolygon => mp.polygons.map(rasterizePolygon(_, tile, re))
    }
  }
}
