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

package geotrellis.pointcloud.layer

import geotrellis.raster.TileLayout
import geotrellis.layer._
import geotrellis.vector.Extent

case class LayoutDefinition3D(extent: Extent,
                              tileLayout: TileLayout,
                              cellDepth: Double,
                              tileLayers: Int,
                              datum: Double = 0.0) {

  def tileCols = tileLayout.tileCols
  def tileRows = tileLayout.tileRows
  def layoutCols = tileLayout.layoutCols
  def layoutRows = tileLayout.layoutRows
  val layerThickness: Double = tileLayers * cellDepth

  lazy val mapTransform = new MapKeyTransform3D(extent, tileLayout.layoutCols, tileLayout.layoutRows, layerThickness, datum)

  def layout2D = LayoutDefinition(extent, tileLayout)
}

object LayoutDefinition3D {
  def apply(layoutDefinition: LayoutDefinition, layerThickness: Double, tileLayers: Int): LayoutDefinition3D =
    LayoutDefinition3D(layoutDefinition.extent, layoutDefinition.tileLayout, layerThickness, tileLayers)

  def apply(layoutDefinition: LayoutDefinition, layerThickness: Double, tileLayers: Int, datum: Double): LayoutDefinition3D =
    LayoutDefinition3D(layoutDefinition.extent, layoutDefinition.tileLayout, layerThickness, tileLayers, datum)
}
