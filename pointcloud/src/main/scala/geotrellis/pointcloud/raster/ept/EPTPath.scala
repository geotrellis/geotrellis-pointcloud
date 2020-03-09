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

import geotrellis.raster.SourcePath

import cats.syntax.option._
import io.lemonlabs.uri.Uri
import io.lemonlabs.uri.encoding.PercentEncoder
import io.lemonlabs.uri.encoding.PercentEncoder.PATH_CHARS_TO_ENCODE

import java.net.MalformedURLException

/** Represents a VALID path that points to an Entwine catalog to be read
  *
  *  @param value Path to an Entwine catalog.
  *
  *  @example "data/my-data.tiff"
  *  @example "ept://data/my-data.tiff"
  *  @example "ept+file://data/my-data.tiff"
  *  @example "ept+s3://bucket/prefix/data.tif"
  *  @example "ept+file:///tmp/data.tiff"
  *
  *  @note Capitalization of the extension is not regarded.
  */
case class EPTPath(value: String) extends SourcePath

object EPTPath {
  val PREFIX = "ept+"

  implicit def toEPTPath(path: String): EPTPath = parse(path)

  def parseOption(path: String, percentEncoder: PercentEncoder = PercentEncoder(PATH_CHARS_TO_ENCODE ++ Set('%', '?', '#'))): Option[EPTPath] = {
    val upath = percentEncoder.encode(path, "UTF-8")
    Uri.parseOption(upath).fold(Option.empty[EPTPath]) { uri =>
      EPTPath(uri.schemeOption.fold(uri.toStringRaw) { scheme =>
        uri.withScheme(scheme.split("\\+").filterNot(_ == "ept").last).toStringRaw
      }).some
    }
  }

  def parse(path: String, percentEncoder: PercentEncoder = PercentEncoder(PATH_CHARS_TO_ENCODE ++ Set('%', '?', '#'))): EPTPath =
    parseOption(path, percentEncoder).getOrElse(throw new MalformedURLException(s"Unable to parse GeoTiffDataPath: $path"))
}