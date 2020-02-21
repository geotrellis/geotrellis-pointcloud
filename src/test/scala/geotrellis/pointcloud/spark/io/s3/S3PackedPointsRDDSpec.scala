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

package geotrellis.pointcloud.spark.io.s3

import geotrellis.pointcloud.spark.PointCloudTestEnvironment
import geotrellis.store.s3.S3ClientProducer
import io.pdal.pipeline.Read
import spire.syntax.cfor._
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest

import java.nio.file.{Files, Paths}

import org.scalatest._

class S3PackedPointsRDDSpec extends FunSpec with Matchers with PointCloudTestEnvironment with BeforeAndAfterAll {
  val bucket = this.getClass.getSimpleName.toLowerCase
  val bucketKey = "las/1.2-with-color.las"
  val keyFiles = "las/files/"

  override def beforeAll(): Unit = {
    S3TestUtils.cleanBucket(MockS3Client.instance, bucket)
    val key = bucketKey
    val filePath = s"${testResources.getAbsolutePath}/las/1.2-with-color.las"
    val fileBytes = Files.readAllBytes(Paths.get(filePath))

    val request = PutObjectRequest.builder()
      .bucket(bucket)
      .key(key)
      .build()

    MockS3Client.instance.putObject(request, RequestBody.fromBytes(fileBytes))

    (1 to 4).foreach { i =>
      val key = s"las/files/1.2-with-color_$i.las"
      val filePath = s"${testResources.getAbsolutePath}/las/files/1.2-with-color_$i.las"
      val fileBytes = Files.readAllBytes(Paths.get(filePath))

      val request = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .build()

      MockS3Client.instance.putObject(request, RequestBody.fromBytes(fileBytes))
    }

    S3ClientProducer.set(() => MockS3Client.instance)
  }

  override def afterAll(): Unit = S3TestUtils.cleanBucket(MockS3Client.instance, bucket)

  describe("PackedPoints RDD reads") {
    val key = bucketKey

    it("should read LAS file as RDD using s3 input format") {
      val source = S3PointCloudRDD(bucket, key).flatMap(_._2)
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

    it("should read multiple LAS files as RDD using s3 input format") {
      val source = S3PointCloudRDD(bucket, keyFiles).flatMap(_._2)
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
      val sourceHeader = S3PointCloudRDD(bucket, key).take(1).head._1
      sourceHeader.crs.map(_.proj4jCrs.getName) should be (Some("lcc-CS"))
    }

    ignore("should read LAS file as RDD using s3 input format (not mock)") {
      val source = S3PointCloudRDD(
        "geotrellis-test", "daunnc/pointcloud/1.2-with-color.las", S3PointCloudRDD.Options(pipeline = Read("s3"))
      ).flatMap(_._2)
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
  }
}
