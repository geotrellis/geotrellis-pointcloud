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

package geotrellis.pointcloud.store.avro.codecs

import geotrellis.store.avro._

import io.pdal.{DimType, PointCloud, SizedDimType}
import org.apache.avro.generic.GenericRecord
import org.apache.avro.util.Utf8
import org.apache.avro.{Schema, SchemaBuilder}
import org.locationtech.jts.geom.Coordinate

import scala.collection.JavaConverters._

import java.nio.ByteBuffer
import java.util

trait PointCloudCodecs extends Serializable {
  implicit def coordinateCodec = new AvroRecordCodec[Coordinate] {
    def schema: Schema = SchemaBuilder
      .record("Coordinate").namespace("org.locationtech.jts.geom")
      .fields()
      .name("x").`type`().doubleType().noDefault()
      .name("y").`type`().doubleType().noDefault()
      .name("z").`type`().doubleType().noDefault()
      .endRecord()

    def encode(c: Coordinate, rec: GenericRecord): Unit = {
      rec.put("x", c.getX)
      rec.put("y", c.getY)
      rec.put("z", c.getZ)
    }

    def decode(rec: GenericRecord): Coordinate =
      new Coordinate(
        rec[Double]("x"),
        rec[Double]("y"),
        rec[Double]("z")
      )
  }

  implicit def arrayCoordinateCodec = new AvroRecordCodec[Array[Coordinate]] {
    def schema: Schema = SchemaBuilder
      .record("ArrayCoordinate").namespace("org.locationtech.jts.geom")
      .fields()
      .name("arr").`type`.array().items.`type`(coordinateCodec.schema).noDefault()
      .endRecord()

    def encode(arr: Array[Coordinate], rec: GenericRecord): Unit = {
      rec.put("arr", java.util.Arrays.asList(arr.map(coordinateCodec.encode):_*))
    }

    def decode(rec: GenericRecord): Array[Coordinate] =
      rec.get("arr")
        .asInstanceOf[java.util.Collection[GenericRecord]]
        .asScala
        .map(coordinateCodec.decode)
        .toArray
  }

  implicit def dimTypeCodec = new AvroRecordCodec[DimType] {
    def schema: Schema = SchemaBuilder
      .record("DimType").namespace("io.pdal")
      .fields()
      .name("id").`type`().stringType().noDefault()
      .name("type").`type`().stringType().noDefault()
      .endRecord()

    def encode(dt: DimType, rec: GenericRecord): Unit = {
      rec.put("id", dt.id)
      rec.put("type", dt.`type`)
    }

    def decode(rec: GenericRecord): DimType =
      DimType(
        rec[Utf8]("id").toString,
        rec[Utf8]("type").toString
      )
  }

  implicit def sizedDimTypeCodec = new AvroRecordCodec[SizedDimType] {
    def schema: Schema = SchemaBuilder
      .record("SizedDimType").namespace("io.pdal")
      .fields()
      .name("dimType").`type`(dimTypeCodec.schema).noDefault()
      .name("size").`type`().longType().noDefault()
      .name("offset").`type`().longType().noDefault()
      .endRecord()

    def encode(sdt: SizedDimType, rec: GenericRecord): Unit = {
      rec.put("dimType", dimTypeCodec.encode(sdt.dimType))
      rec.put("size", sdt.size)
      rec.put("offset", sdt.offset)
    }

    def decode(rec: GenericRecord): SizedDimType =
      SizedDimType(
        dimTypeCodec.decode(rec[GenericRecord]("dimType")),
        rec[Long]("size"),
        rec[Long]("offset")
      )
  }

  implicit def pointCloudCodec = new AvroRecordCodec[PointCloud] {
    def schema: Schema = SchemaBuilder
      .record("PointCloud").namespace("io.pdal")
      .fields()
      .name("bytes").`type`().bytesType().noDefault()
      .name("dimTypes").`type`().map().values(sizedDimTypeCodec.schema).noDefault()
      .endRecord()

    def encode(pc: PointCloud, rec: GenericRecord): Unit = {
      rec.put("bytes", ByteBuffer.wrap(pc.bytes))

      val dimTypes = new util.HashMap[String, GenericRecord]()
      pc.dimTypes.asScala.foreach { case (k, v) => dimTypes.put(k, sizedDimTypeCodec.encode(v)) }

      rec.put("dimTypes", dimTypes)
    }

    def decode(rec: GenericRecord): PointCloud = {
      val dimTypes = new util.HashMap[String, SizedDimType]()
      rec[util.Map[Utf8, GenericRecord]]("dimTypes")
        .asScala
        .foreach { case (k, v) => dimTypes.put(k.toString, sizedDimTypeCodec.decode(v)) }

      PointCloud(
        rec[ByteBuffer]("bytes").array,
        dimTypes
      )
    }
  }
}
