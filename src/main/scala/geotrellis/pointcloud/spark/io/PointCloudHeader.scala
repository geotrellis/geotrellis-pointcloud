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

package geotrellis.pointcloud.spark.io

import geotrellis.pointcloud.spark.json._
import geotrellis.pointcloud.vector.{Extent3D, ProjectedExtent3D}
import geotrellis.proj4.CRS
import geotrellis.vector.Extent

import io.circe.parser._
import cats.syntax.either._
import io.pdal.pipeline.ReaderTypes

trait PointCloudHeader {
  val metadata: String
  val schema: String

  def projectedExtent3D: Option[ProjectedExtent3D] =
    parse(metadata).right.flatMap(_.as[ProjectedExtent3D]).toOption

  def extent3D: Option[Extent3D] = projectedExtent3D.map(_.extent3d)
  def extent: Option[Extent] = projectedExtent3D.map(_.extent3d.toExtent)
  def crs: Option[CRS] = {
    val result = projectedExtent3D.map(_.crs)
    if(result.isEmpty) {
      parse(metadata).right.toOption.flatMap { json =>
        val md = json.hcursor.downField("metadata")
        val driver =
          ReaderTypes
            .all.flatMap(s => md.downField(s.toString).focus)
            .headOption
            .map(_.hcursor)
            .getOrElse(throw new Exception(s"Unsupported reader driver: ${md.keys.getOrElse(Nil)}"))

        driver.downField("srs").downField("proj4").focus.map { str =>
          CRS.fromString(str.noSpaces.replace("\"", ""))
        }
      }
    }
    else result
  }
}
