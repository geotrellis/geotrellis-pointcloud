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

package geotrellis.pointcloud.vector

import geotrellis.pointcloud.util.EitherMethods
import geotrellis.vector.{Extent, Point}

import _root_.io.pdal.pipeline.ReaderTypes
import _root_.io.circe.Decoder

case class Extent3D(xmin: Double, ymin: Double, zmin: Double, xmax: Double, ymax: Double, zmax: Double) {
  def toExtent = Extent(xmin, ymin, xmax, ymax)
  def width = xmax - xmin
  def height = ymax - ymin
  def depth = zmax - zmin

  /** Return the smallest extent that contains this extent and the provided extent. */
  def combine(other:Extent3D): Extent3D =
    Extent3D(
      if(xmin < other.xmin) xmin else other.xmin,
      if(ymin < other.ymin) ymin else other.ymin,
      if(zmin < other.zmin) zmin else other.zmin,
      if(xmax > other.xmax) xmax else other.xmax,
      if(ymax > other.ymax) ymax else other.ymax,
      if(zmax > other.zmax) zmax else other.zmax
    )

  /** Return the smallest extent that contains this extent and the provided extent. */
  def expandToInclude(other: Extent3D): Extent3D =
    combine(other)

  /** Return the smallest extent that contains this extent and the provided point. */
  def expandToInclude(p: Point): Extent3D = {
    val c = p.getCoordinate
    expandToInclude(c.getX, c.getY, c.getZ)
  }

  /** Return the smallest extent that contains this extent and the provided point. */
  def expandToInclude(x: Double, y: Double, z: Double): Extent3D =
    Extent3D(
      if(xmin < x) xmin else x,
      if(ymin < y) ymin else y,
      if(zmin < z) zmin else z,
      if(xmax > x) xmax else x,
      if(ymax > y) ymax else y,
      if(zmax > z) zmax else z
    )

}

object Extent3D {
  implicit val extent3DDecoder: Decoder[Extent3D] = Decoder.instance { cursor =>
    val md = cursor.downField("metadata")
    val driver =
      ReaderTypes
        .all.flatMap(s => md.downField(s.toString).focus)
        .headOption
        .map(_.hcursor)
        .getOrElse(throw new Exception(s"Unsupported reader driver: ${md.keys.getOrElse(Nil)}"))

    lazy val info =
      md
        .downField("filters.info")
        .downField("bbox")
        .focus
        .map(_.hcursor)
        .getOrElse(throw new Exception(s"Unsupported reader driver: ${md.keys.getOrElse(Nil)}"))

    lazy val resultInfo = EitherMethods.sequence(
      info.downField("minx").as[Double] ::
        info.downField("miny").as[Double] ::
        info.downField("minz").as[Double] ::
        info.downField("maxx").as[Double] ::
        info.downField("maxy").as[Double] ::
        info.downField("maxz").as[Double] :: Nil
    ).right.map { case List(xmin, ymin, zmin, xmax, ymax, zmax) =>
      Extent3D(xmin, ymin, zmin, xmax, ymax, zmax)
    }

    val result = EitherMethods.sequence(
      driver.downField("minx").as[Double] ::
        driver.downField("miny").as[Double] ::
        driver.downField("minz").as[Double] ::
        driver.downField("maxx").as[Double] ::
        driver.downField("maxy").as[Double] ::
        driver.downField("maxz").as[Double] :: Nil
    ).right.map { case List(xmin, ymin, zmin, xmax, ymax, zmax) =>
      Extent3D(xmin, ymin, zmin, xmax, ymax, zmax)
    }

    if(result.isRight) result
    else resultInfo
  }
}
