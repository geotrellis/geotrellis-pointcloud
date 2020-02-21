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

package geotrellis.pointcloud.vector.triangulation

import geotrellis.vector.Point

import org.locationtech.jts.geom.Coordinate

trait DelaunayPointSet {
  def length: Int
  def getX(i: Int): Double
  def getY(i: Int): Double
  def getZ(i: Int): Double
  def getCoordinate(i: Int): Coordinate = new Coordinate(getX(i), getY(i), getZ(i))
  def getPoint(i: Int): Point = Point(getCoordinate(i))
  def apply(i: Int): Coordinate = getCoordinate(i)
  def distance(i1: Int, i2: Int): Double = {
    val dx = getX(i1) - getX(i2)
    val dy = getY(i1) - getY(i2)

    math.sqrt((dx * dx) + (dy * dy))
  }
}

object DelaunayPointSet {

  def apply(points: Array[Coordinate]): DelaunayPointSet =
    new DelaunayPointSet {
      def length = points.length
      def getX(i: Int) = points(i).getX
      def getY(i: Int) = points(i).getY
      def getZ(i: Int) = points(i).getZ
      override def getCoordinate(i: Int) = points(i)
    }

  def apply(points: Map[Int, Coordinate]): DelaunayPointSet =
    apply(points, points.size)

  def apply(points: Int => Coordinate, len: Int): DelaunayPointSet =
    new DelaunayPointSet {
      def length = len
      def getX(i: Int) = points(i).getX
      def getY(i: Int) = points(i).getY
      def getZ(i: Int) = points(i).getZ
      override def getCoordinate(i: Int) = points(i)
    }

  implicit def coordinateArrayToDelaunayPointSet(points: Array[Coordinate]): DelaunayPointSet =
    apply(points)
}
