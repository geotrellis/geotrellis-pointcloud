package geotrellis.pointcloud.spark

import _root_.io.pdal._
import org.apache.spark.sql.types.{DataType, DataTypes, StructField, StructType}
import spire.syntax.cfor.cfor

import scala.collection.JavaConverters._

package object datasource {
  implicit class withPointCloudMethods(pc: PointCloud) {
    lazy val sortedDimTypes: List[(String, SizedDimType)] = pc.dimTypes.asScala.toList.sortBy(_._2.offset)

    def structFieldType(sd: SizedDimType): DataType = {
      sd.dimType.`type` match {
        case "byte" | "ubyte" | "byte_t" | "ubyte_t" |
             "int8_t" | "int8" | "uint8_t" | "uint8"     => DataTypes.ByteType
        case "uint16_t" | "uint16" | "int16_t" | "int16" => DataTypes.ShortType
        case "int32_t" | "int32"                         => DataTypes.IntegerType
        case "uint32_t" | "uint32" | "uint64_t" | "uint64" |
             "int32_t" | "int32" | "int64_t" | "int64"   => DataTypes.LongType
        case "float"                                     => DataTypes.FloatType
        case "double"                                    => DataTypes.DoubleType
      }
    }

    def deriveSchema: StructType =
      StructType(sortedDimTypes.map { case (name, sd) => StructField(name, structFieldType(sd), nullable = true) } )

    def readUntyped(idx: Int, sd: SizedDimType): Any = {
      val buffer = pc.get(idx, sd)
      sd.dimType.`type` match {
        case "byte" | "ubyte" | "byte_t" | "ubyte_t" |
             "int8_t" | "int8" | "uint8_t" | "uint8"     => buffer.get()
        case "uint16_t" | "uint16" | "int16_t" | "int16" => buffer.getShort()
        case "int32_t" | "int32"                         => buffer.getInt
        case "uint32_t" | "uint32" | "uint64_t" | "uint64" |
             "int32_t" | "int32" | "int64_t" | "int64"   => buffer.getLong
        case "float"                                     => buffer.getFloat
        case "double"                                    => buffer.getDouble
      }
    }

    def readAll: Array[Array[Any]] = {
      val rowArray = Array.ofDim[Any](pc.length, sortedDimTypes.length)
      val indexedSortedDimTypes = sortedDimTypes.zipWithIndex
      cfor(0)(_ < pc.length, _ + 1) { idx =>
        indexedSortedDimTypes.foreach { case ((_, dim), dimIdx) =>
          rowArray(idx)(dimIdx) = readUntyped(idx, dim)
        }
      }

      rowArray
    }
  }
}
