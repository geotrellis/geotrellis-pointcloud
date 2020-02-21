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

package geotrellis.pointcloud.vector.triangulation

import geotrellis.vector.{Extent, Point}

case class LightPoint(x: Double, y: Double, z: Double = 0.0) {
  def normalized(e: Extent): LightPoint = LightPoint((x - e.xmin) / e.width, (y - e.ymin) / e.height, z)

  def distance(other: LightPoint): Double = {
    val dx = other.x - x
    val dy = other.y - y
    math.sqrt(dx * dx + dy * dy)
  }

  def toPoint: Point = Point(x, y)
}
