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
import cats.syntax.either._

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
    val json = parse(raw).valueOr(throw _)
    val table = json.asObject.get.toList.toMap.mapValues(_.toString.toLong)

    val recurseKeys = table.filter(_._2 == -1).keys.toList
    val joined = (table -- recurseKeys).groupBy(_._1.split("-").head.toInt).mapValues(_.values.sum)
    val nested = recurseKeys.map(pointsInLevels(base, _))

    nested.fold(joined)(_ combine _)
  }

  private def approxPointsPerTile(base: URI): Long = {
    val rr = RangeReader(base.resolve(s"ept-hierarchy/0-0-0-0.json").toString)
    val raw = new String(rr.readAll)
    val json = parse(raw).valueOr(throw _)
    val table = json.asObject.get.toList.toMap.mapValues(_.toString.toLong)

    val nontrivials = table.filterNot(_._2 == -1)
    val avgCnt = nontrivials.map(_._2).sum / nontrivials.size

    avgCnt
  }

  def apply(source: String, withHierarchy: Boolean = false): EPTMetadata = {
    val src = if (source.endsWith("/")) source else s"$source/"
    val raw = Raw(src)
    val uri = new URI(src)
    val (counts, maxDepth) = {
      if(withHierarchy) {
        val cnts = pointsInLevels(uri, "0-0-0-0").toList.sorted
        cnts -> cnts.last._1
      } else {
        val appt = approxPointsPerTile(uri)
        val approxTileCount = raw.points / appt

        // Assume that geometry is "flat", tree is more quad-tree like
        val fullLevels = math.log(approxTileCount) / math.log(4)

        (Map.empty[Int, Long], (1.5 * fullLevels).toInt)
      }
    }

    // https://github.com/PDAL/PDAL/blob/2.1.0/io/EptReader.cpp#L293-L318
    val resolutions = (maxDepth to 0).by(-1).toList.map { l =>
      CellSize((raw.extent.width / raw.span) / math.pow(2, l), (raw.extent.height / raw.span) / math.pow(2, l))
    }

    EPTMetadata(
      StringName(src),
      raw.srs.toCRS(),
      DoubleCellType,
      GridExtent[Long](raw.extent, resolutions.head),
      resolutions,
      Map(
        "points" -> raw.points.toString,
        "pointsInLevels" -> counts.map(_._2).mkString(","),
        "minz" -> raw.boundsConforming(2).toString,
        "maxz" -> raw.boundsConforming(5).toString
      )
    )
  }
}
