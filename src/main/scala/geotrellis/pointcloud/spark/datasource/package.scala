package geotrellis.pointcloud.spark

import cats.syntax.either._
import _root_.io.pdal._
import _root_.io.circe._
import _root_.io.circe.parser._
import _root_.io.circe.syntax._

import geotrellis.pointcloud.vector.Extent3D
import geotrellis.proj4.CRS
import geotrellis.vector.Extent

import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder
import org.apache.spark.sql.types._
import spire.syntax.cfor.cfor

import scala.collection.JavaConverters._
import scala.util.Try

package object datasource {
  implicit val crsEncoder: _root_.io.circe.Encoder[CRS] =
    Encoder.encodeString.contramap[CRS] { crs =>
      crs.epsgCode
        .map { c =>
          s"epsg:$c"
        }
        .getOrElse(crs.toProj4String)
    }

  implicit val crsDecoder: Decoder[CRS] =
    Decoder.decodeString.emap { str =>
      Either
        .catchNonFatal(Try(CRS.fromName(str)) getOrElse CRS.fromString(str))
        .leftMap(_ => "CRS")
    }

  implicit val extentEncoder: _root_.io.circe.Encoder[Extent] =
    new _root_.io.circe.Encoder[Extent] {
      def apply(extent: Extent): Json =
        List(extent.xmin, extent.ymin, extent.xmax, extent.ymax).asJson
    }
  implicit val extentDecoder: Decoder[Extent] =
    Decoder[Json] emap { js =>
      js.as[List[Double]]
        .map {
          case List(xmin, ymin, xmax, ymax) =>
            Extent(xmin, ymin, xmax, ymax)
        }
        .leftMap(_ => "Extent")
    }

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

    // TODO: name.replace("\"", "") should be probably removed
    def deriveSchema(metadata: Metadata): StructType =
      StructType(sortedDimTypes.map { case (name, sd) =>
        StructField(name.replace("\"", ""), structFieldType(sd), nullable = true, metadata)
      })

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

  implicit val extent3DEncoder: ExpressionEncoder[Extent3D] = ExpressionEncoder[Extent3D]()

  implicit class withPointCloudFramesMethods(self: DataFrame) {
   def findField(name: String): Option[StructField] =
     self.schema.fields.find { _.metadata.contains(name) }

    def metadata: (Option[Extent], Option[CRS]) =
      self
        .findField("metadata")
        .map(_.metadata)
        .map(_.getString("metadata"))
        .flatMap(parse(_).toOption)
        .flatMap(_.as[(Option[Extent], Option[CRS])].toOption)
        .getOrElse(throw new Exception("Not a PointCloud DataFrame"))
  }
}
