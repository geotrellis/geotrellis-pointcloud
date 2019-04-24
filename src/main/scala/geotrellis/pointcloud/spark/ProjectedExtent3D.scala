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

package geotrellis.pointcloud.spark

import geotrellis.proj4.CRS

import _root_.io.pdal.pipeline.ReaderTypes
import _root_.io.circe.Decoder

case class ProjectedExtent3D(extent3d: Extent3D, crs: CRS)

object ProjectedExtent3D {
  implicit val projectedExtent3DDecoder: Decoder[ProjectedExtent3D] = Decoder.instance { cursor =>
    val md = cursor.downField("metadata")
    val driver =
      ReaderTypes
        .all.flatMap(s => md.downField(s.toString).focus)
        .headOption
        .map(_.hcursor)
        .getOrElse(throw new Exception(s"Unsupported reader driver: ${md.keys.getOrElse(Nil)}"))

    lazy val proj4StringInfo =
      md
        .downField("filters.info")
        .downField("srs")
        .focus
        .map(_.hcursor)
        .getOrElse(throw new Exception(s"Unsupported reader driver: ${md.keys.getOrElse(Nil)}"))

    val proj4String = driver.downField("srs").downField("proj4").as[String]

    val result = if(proj4String.isRight) proj4String else proj4StringInfo.downField("srs").downField("proj4").as[String]

    val crs =
      CRS.fromString(result match {
        case Right(s) => s
        case Left(e) => throw new Exception("Incorrect CRS metadata information, try to provide the input CRS").initCause(e)
      })

    cursor.value.as[Extent3D].right.map(ProjectedExtent3D(_, crs))
  }
}
