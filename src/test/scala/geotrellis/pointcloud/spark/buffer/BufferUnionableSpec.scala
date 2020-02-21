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

import geotrellis.layer._
import geotrellis.util._

import geotrellis.spark.testkit._

import org.scalatest.FunSpec

object Stuff {
  case class UnionableThing(n: Int) {
    def union(other: Any): UnionableThing = {
      other match {
        case that: UnionableThing => UnionableThing(this.n * that.n)
        case _ => throw new Exception
      }
    }
  }
}

class BufferUnionableSpec extends FunSpec with TestEnvironment {

  import Stuff.UnionableThing

  describe("General BufferTiles functionality") {
    it("should union neighbors, not union non-neighbors") {
      val key1 = SpatialKey(0,0)
      val key2 = SpatialKey(1,1)
      val key3 = SpatialKey(13, 33)
      val thing1 = UnionableThing(47)
      val thing2 = UnionableThing(53)
      val thing3 = UnionableThing(59)

      val rdd = sc.parallelize(List((key1, thing1), (key2, thing2), (key3, thing3)))
      val results = BufferUnionable(rdd).map({ case (k, thing) => k -> thing.n }).collect.toMap

      results(key1) should be (47 * 53)
      results(key2) should be (47 * 53)
      results(key3) should be (59)
    }
  }

}
