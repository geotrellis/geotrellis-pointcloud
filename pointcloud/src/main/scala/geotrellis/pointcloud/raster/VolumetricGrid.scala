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

package geotrellis.pointcloud.raster

trait VolumetricGrid extends Serializable {
  def cols: Int
  def rows: Int
  def layers: Int

  /**
    * The size of the grid, e.g. cols * rows * layers.
    */
  def size: Int = cols * rows * layers
  def dimensions: (Int, Int, Int) = (cols, rows, layers)
  def gridBounds: GridBounds3D[Int] = GridBounds3D(0, 0, 0, cols - 1, rows - 1, layers - 1)
}
