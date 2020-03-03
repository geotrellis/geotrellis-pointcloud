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

import geotrellis.util.RangeReader
import geotrellis.vector.Extent

import cats.syntax.either._
import io.circe.generic.JsonCodec
import io.circe.parser.decode

import java.net.URI

@JsonCodec
case class Raw(
  bounds: Seq[Double],
  boundsConforming: Seq[Double],
  dataType: String,
  hierarchyType: String,
  points: Long,
  schema: Seq[Field],
  span: Int,
  srs: SRS,
  version: String
) {
  def extent: Extent = {
    val Seq(xmin, ymin, _, xmax, ymax, _) = bounds
    Extent(xmin, ymin, xmax, ymax)
  }
}

object Raw {
  def apply(eptSource: String): Raw = {
    val rr = RangeReader(new URI(eptSource).resolve("ept.json").toString)
    val jsonString = new String(rr.readAll)
    decode[Raw](jsonString).valueOr(throw _)
  }
}
