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

package geotrellis.pointcloud.spark.buffer

import geotrellis.spark._

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

object BufferUnionable {

  /**
    * Given an RDD of (K, V) pairs, union each object with its
    * neighbors.  The "neighbor" relationship is given by the keys.
    *
    * @tparam  K    The key type.
    * @tparam  V    The value type; must be unionable.
    *
    * @param   rdd  An RDD of K-V pairs.
    * @return       An RDD of K-V pairs where each V has been unioned with its neighbors.
    */
  def apply[
    K: SpatialComponent,
    X <: { def union(other: Any): V },
    V: (? => X) : ClassTag
  ](rdd: RDD[(K, V)]): RDD[(K, V)] = {
    rdd
      .flatMap({ case (key, data) =>
        val SpatialKey(col, row) = key

        for (deltaX <- -1 to +1; deltaY <- -1 to +1) yield {
          if (deltaX == 0 && deltaY == 0)
            (SpatialKey(col + deltaX, row + deltaY), (key, data, true))
          else
            (SpatialKey(col + deltaX, row + deltaY), (key, data, false))
        }
      })
      .groupByKey
      .filter({ case (_, seq) => seq.exists { case (_, _, center) => center } })
      .map({ case (sortKey, seq) =>
        val resultKey = seq.filter({ case (_, _, center) => center }).head._1
        val resultValue = seq.map({ case (_, data, _) => data }).reduce(_ union _)

        (resultKey, resultValue)
      })
  }

}
