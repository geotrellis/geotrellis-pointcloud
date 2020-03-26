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
import geotrellis.util.RangeReader

import _root_.io.circe.parser._
import cats.instances.long._
import cats.instances.map._
import cats.syntax.semigroup._

import java.net.URI

case class EPTMetadata(
  name: SourceName,
  crs: CRS,
  cellType: CellType,
  gridExtent: GridExtent[Long],
  resolutions: List[CellSize],
  attributes: Map[String, String]
) extends RasterMetadata {
  val bandCount = 1
  def attributesForBand(i: Int): Map[String, String] = Map.empty
}

object EPTMetadata {
  def pointsInLevels(base: URI, key: String): Map[Int, Long] = {
    val rr = RangeReader(base.resolve(s"ept-hierarchy/$key.json").toString)
    val raw = new String(rr.readAll)
    val Right(json) = parse(raw)
    val table = json.asObject.get.toList.toMap.mapValues(_.toString.toLong)

    val recurseKeys = table.filter(_._2 == -1).keys.toList
    val joined = (table -- recurseKeys).groupBy(_._1.split("-").head.toInt).mapValues(_.values.sum)
    val nested = recurseKeys.map(pointsInLevels(base, _))

    nested.fold(joined)(_ combine _)
  }

  def tree(source: String): EPTMetadata = {
    val src = if (source.endsWith("/")) source else s"$source/"
    val raw = Raw(src)
    val counts = pointsInLevels(new URI(src), "0-0-0-0")
    val maxDepth = counts.keys.max

    // https://github.com/PDAL/PDAL/blob/2.1.0/io/EptReader.cpp#L293-L318
    val resolutions = (0 to maxDepth).toList.map { l =>
      CellSize((raw.extent.width / raw.span) / math.pow(2, l), (raw.extent.height / raw.span) / math.pow(2, l))
    }

    EPTMetadata(
      StringName(src),
      raw.srs.toCRS(),
      DoubleCellType,
      GridExtent[Long](raw.extent, raw.span, raw.span),
      resolutions,
      Map(
        "points" -> raw.points.toString,
        "pointsInLevels" -> counts.toSeq.sorted.map(_._2).mkString(","),
        "minz" -> raw.boundsConforming(2).toString,
        "maxz" -> raw.boundsConforming(5).toString
      )
    )
  }

  def pointsInLevel(base: URI, key: String): Map[Int, Long] = {
    val rr = RangeReader(base.resolve(s"ept-hierarchy/$key.json").toString)
    val raw = new String(rr.readAll)
    val Right(json) = parse(raw)
    val table = json.asObject.get.toList.toMap.mapValues(_.toString.toLong)
    val recurseKeys = table.filter(_._2 == -1).keys.toList
    (table -- recurseKeys).groupBy(_._1.split("-").head.toInt).mapValues(_.values.sum)
  }

  def apply(source: String): EPTMetadata = {
    val src = if (source.endsWith("/")) source else s"$source/"
    val raw = Raw(src)
    val counts = pointsInLevel(new URI(src), "0-0-0-0")
    val maxDepth = counts.keys.max

    // https://github.com/PDAL/PDAL/blob/2.1.0/io/EptReader.cpp#L293-L318
    val resolutions = (0 to maxDepth).toList.map { l =>
      CellSize((raw.extent.width / raw.span) / math.pow(2, l), (raw.extent.height / raw.span) / math.pow(2, l))
    }

    EPTMetadata(
      StringName(src),
      raw.srs.toCRS(),
      DoubleCellType,
      GridExtent[Long](raw.extent, raw.span, raw.span),
      resolutions,
      Map(
        "points" -> raw.points.toString,
        "pointsInLevels" -> counts.toSeq.sorted.map(_._2).mkString(","),
        "minz" -> raw.boundsConforming(2).toString,
        "maxz" -> raw.boundsConforming(5).toString
      )
    )
  }
}
