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

import geotrellis.raster.{RasterSource, RasterSourceProvider}

class EPTRasterSourceProvider extends RasterSourceProvider {
  // TIN is a default fallback
  def isTin(path: String): Boolean =
    path.nonEmpty && (path.startsWith(EPTPath.PREFIX_TIN) || path.startsWith(EPTPath.PREFIX) || path.startsWith(EPTPath.SCHEME))
  def isIdw(path: String): Boolean =
    path.nonEmpty && path.startsWith(EPTPath.PREFIX_IDW) && (path.contains(EPTPath.PREFIX) || path.contains(EPTPath.SCHEME))

  def canProcess(path: String): Boolean = isTin(path) || isIdw(path)
  def rasterSource(path: String): RasterSource = if(isIdw(path)) IDWRasterSource(path) else TINRasterSource(path)
}
