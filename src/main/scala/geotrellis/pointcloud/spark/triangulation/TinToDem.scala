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

package geotrellis.pointcloud.spark.triangulation

import geotrellis.pointcloud.raster._
import geotrellis.raster._
import geotrellis.raster.triangulation.DelaunayRasterizer
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.vector._
import geotrellis.vector.triangulation._
import geotrellis.vector.mesh.IndexedPointSet
import geotrellis.spark.buffer

import org.locationtech.jts.geom.Coordinate
import org.apache.spark.rdd.RDD
import spire.syntax.cfor._

object TinToDem {
  case class Options(
    cellType: CellType = DoubleConstantNoDataCellType,
    boundsBuffer: Option[Double] = None
  )

  object Options {
    def DEFAULT = Options()
  }

  def apply(rdd: RDD[(SpatialKey, Array[Coordinate])], layoutDefinition: LayoutDefinition, options: Options = Options.DEFAULT): RDD[(SpatialKey, Tile)] =
    rdd
      .collectNeighbors
      .mapPartitions({ partition =>
        partition.flatMap { case (key, neighbors) =>
          val extent @ Extent(xmin, ymin, xmax, ymax) =
            layoutDefinition.mapTransform(key)

          var len = 0
          neighbors.foreach { case (_, (_, arr)) =>
            len += arr.length
          }
          val points = Array.ofDim[Coordinate](len)
          var j = 0
          options.boundsBuffer match {
            case Some(b) =>
              val bufferedExtent =
                Extent(
                  xmin - b,
                  ymin - b,
                  xmax + b,
                  ymax + b
                )

              neighbors.foreach { case (_, (_, tilePoints)) =>
                cfor(0)(_ < tilePoints.length, _ + 1) { i =>
                  val p = tilePoints(i)
                  // Only consider points within `boundsBuffer` of the extent
                  if(bufferedExtent.contains(p.x, p.y)) {
                    points(j) = p
                    j += 1
                  }
                }
              }
            case None =>
              neighbors.foreach { case (_, (_, tilePoints)) =>
                cfor(0)(_ < tilePoints.length, _ + 1) { i =>
                  points(j) = tilePoints(i)
                  j += 1
                }
              }
          }

          if(j > 2) {
            val pointSet =
              IndexedPointSet(points)

            val delaunay = DelaunayTriangulation(pointSet)

            val re =
              RasterExtent(
                extent,
                layoutDefinition.tileCols,
                layoutDefinition.tileRows
              )

            val tile =
              ArrayTile.empty(options.cellType, re.cols, re.rows)

            DelaunayRasterizer.rasterizeDelaunayTriangulation(delaunay, re, tile)

            Some((key, tile))
          } else {
            None
          }
        }
      }, preservesPartitioning = true)

  def boundaryStitch(rdd: RDD[(SpatialKey, Array[Coordinate])], layoutDefinition: LayoutDefinition, options: Options = Options.DEFAULT): RDD[(SpatialKey, Tile)] = {

    // Assumes that a partitioner has already been set

    val triangulations: RDD[(SpatialKey, DelaunayTriangulation)] =
      rdd
        .map { case (key, points) =>
          // println(s"Calculating Delaunay triangulation for $key")
          (key, DelaunayTriangulation(points))
        }

    val borders: RDD[(SpatialKey, BoundaryDelaunay)] =
      triangulations
        .mapPartitions{ iter =>
          iter.map{ case (sk, dt) => {
            val ex: Extent = layoutDefinition.mapTransform(sk)
            // println(s"Extracting BoundaryDelaunay for $sk")
            (sk, BoundaryDelaunay(dt, ex))
          }}
        }

    borders
      .collectNeighbors
      .mapPartitions({ partition =>
        partition.map { case (key, neighbors) =>
          val newNeighbors =
            neighbors.map { case (direction, (key2, border)) =>
              val ex = layoutDefinition.mapTransform(key2)
              (buffer.Direction.convertDirection(direction), (border, ex))
            }
          (key, newNeighbors.toMap)
        }
      }, preservesPartitioning = true)
      .join(triangulations)
      .mapPartitions({ partition =>
        partition.map { case (key, (borders, triangulation)) => // : (Map[Direction, (BoundaryDelaunay, Extent)], DelaunayTriangulation)
          val extent = layoutDefinition.mapTransform(key)
          val re =
            RasterExtent(
              extent,
              layoutDefinition.tileCols,
              layoutDefinition.tileRows
            )

          // println(s"Stitching for $key [$extent]:")
          // borders.foreach{ case (dir, (_, ex)) =>
          //   println(s"  Neighbor $dir has $ex")
          // }

          val stitched = StitchedDelaunay(borders)
          val tile = stitched.rasterize(re, options.cellType)(triangulation)

          (key, tile)
        }
      }, preservesPartitioning = true)
  }

  def allStitch(rdd: RDD[(SpatialKey, Array[Coordinate])], layoutDefinition: LayoutDefinition, options: Options = Options.DEFAULT): RDD[(SpatialKey, Tile)] = {

    // Assumes that a partitioner has already been set

    val triangulations: RDD[(SpatialKey, DelaunayTriangulation)] =
      rdd
        .mapValues { points =>
          DelaunayTriangulation(points)
        }

    triangulations
      .collectNeighbors
      .mapPartitions({ partition =>
        partition.map { case (key, neighbors) =>
          val newNeighbors =
            neighbors.map { case (direction, (key2, dt)) =>
              val ex = layoutDefinition.mapTransform(key2)
              (buffer.Direction.convertDirection(direction), (dt, ex))
            }
          (key, newNeighbors.toMap)
        }
      }, preservesPartitioning = true)
      .join(triangulations)
      .mapPartitions({ partition =>
        partition.map { case (key, (nbhd, triangulation)) => // : (Map[Direction, (BoundaryDelaunay, Extent)], DelaunayTriangulation)
          val stitched = StitchedDelaunay(nbhd, false)

          val extent = layoutDefinition.mapTransform(key)
          val re =
            RasterExtent(
              extent,
              layoutDefinition.tileCols,
              layoutDefinition.tileRows
            )

          val tile = stitched.rasterize(re, options.cellType)(triangulation)

          (key, tile)
        }
      }, preservesPartitioning = true)
  }
}
