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

package geotrellis.pointcloud.spark.dem

import io.pdal._
import geotrellis.layer._
import geotrellis.util._
import org.apache.spark.rdd.RDD

object Implicits extends Implicits

trait Implicits {
  implicit class withPointCloudToDemMethods[M: GetComponent[*, LayoutDefinition]](
    self: RDD[(SpatialKey, PointCloud)] with Metadata[M]
  ) extends PointCloudToDemMethods[M](self)

  implicit class withPointCloudDemMethods(val self: PointCloud)
      extends PointCloudDemMethods
}
