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

package geotrellis.pointcloud.spark.io.hadoop

import geotrellis.pointcloud.spark._
import geotrellis.layer.{SpatialKey, TileLayerMetadata}
import geotrellis.spark._
import geotrellis.store.{InvalidLayerIdError, LayerId}
import geotrellis.store.hadoop._
import geotrellis.spark.store.hadoop._
import geotrellis.store.index._
import geotrellis.pointcloud.spark.io.avro.Implicits._
import geotrellis.spark.testkit.io._
import geotrellis.spark.testkit.testfiles.TestFiles

import org.locationtech.jts.geom.Coordinate

class HadoopArrayCoordinateSpatialSpec
  extends PersistenceSpec[SpatialKey, Array[Coordinate], TileLayerMetadata[SpatialKey]]
    with SpatialKeyIndexMethods
    with PointCloudTestEnvironment
    with TestFiles
    with PointCloudSpatialTestFiles {

  lazy val reader = HadoopLayerReader(outputLocal)
  lazy val creader = HadoopCollectionLayerReader(outputLocal)
  lazy val writer = HadoopLayerWriter(outputLocal)
  lazy val deleter = HadoopLayerDeleter(outputLocal)
  lazy val copier = HadoopLayerCopier(outputLocal)
  lazy val mover  = HadoopLayerMover(outputLocal)
  lazy val reindexer = HadoopLayerReindexer(outputLocal)
  lazy val tiles = HadoopValueReader(outputLocal)
  lazy val sample = pointCloudSampleC

  describe("HDFS layer names") {
    it("should handle layer names with spaces") {
      val layer = sample
      val layerId = LayerId("Some layer", 10)

      writer.write[SpatialKey, Array[Coordinate], TileLayerMetadata[SpatialKey]](layerId, layer, ZCurveKeyIndexMethod)
      val backin = reader.read[SpatialKey, Array[Coordinate], TileLayerMetadata[SpatialKey]](layerId)
    }

    it("should fail gracefully with colon in name") {
      val layer = sample
      val layerId = LayerId("Some:layer", 10)

      intercept[InvalidLayerIdError] {
        writer.write[SpatialKey, Array[Coordinate], TileLayerMetadata[SpatialKey]](layerId, layer, ZCurveKeyIndexMethod)
      }
    }
  }
}
