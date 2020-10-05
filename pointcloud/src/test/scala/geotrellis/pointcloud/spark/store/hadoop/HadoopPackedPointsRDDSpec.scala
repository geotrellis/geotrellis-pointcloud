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

package geotrellis.pointcloud.spark.store.hadoop

import geotrellis.pointcloud.spark.PointCloudTestEnvironment

import spire.syntax.cfor._

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class HadoopPackedPointsRDDSpec extends AnyFunSpec with Matchers with PointCloudTestEnvironment {
  describe("PackedPoints RDD reads") {
    it("should read LAS file as RDD using hadoop input format") {
      val source = HadoopPointCloudRDD(lasPath).flatMap(_._2)
      val pointsCount = source.mapPartitions { _.map { packedPoints =>
        var acc = 0l
        cfor(0)(_ < packedPoints.length, _ + 1) { i =>
          packedPoints.get(i)
          acc += 1
        }
        acc
      } }.reduce(_ + _)
      val sourceList = source.take(1).toList
      sourceList.map(_.length).head should be (1065)
      pointsCount should be (1065)
    }

    it("should read multiple LAS files as RDD using hadoop input format") {
      val source = HadoopPointCloudRDD(multipleLasPath).flatMap(_._2)
      val pointsCount = source.mapPartitions { _.map { packedPoints =>
        var acc = 0l
        cfor(0)(_ < packedPoints.length, _ + 1) { i =>
          packedPoints.get(i)
          acc += 1
        }
        acc
      } }.reduce(_ + _)
      val sourceList = source.take(1).toList
      sourceList.map(_.length).head should be (1065)
      pointsCount should be (4 * 1065)
    }

    it("should read correct crs") {
      val sourceHeader = HadoopPointCloudRDD(lasPath).take(1).head._1
      sourceHeader.crs.map(_.proj4jCrs.getName) should be (Some("lcc-CS"))
    }
  }
}
