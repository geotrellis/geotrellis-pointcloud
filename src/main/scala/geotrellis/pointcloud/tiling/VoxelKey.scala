package geotrellis.pointcloud.tiling

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.spark.io.index.zcurve._
import geotrellis.spark.io.json._
import geotrellis.util._

import spray.json._

// --- //

/** A three-dimensional spatial key. A ''voxel'' is the 3D equivalent of a pixel. */
case class VoxelKey(col: Int, row: Int, layer: Int) {
  def spatialKey = SpatialKey(col, row)
  def depthKey = DepthKey(layer)
}

/** Typeclass instances. These (particularly [[Boundable]]) are necessary
  * for when a layer's key type is parameterized as ''K''.
  */
object VoxelKey {
  implicit def ordering[A <: VoxelKey]: Ordering[A] =
    Ordering.by(k => (k.col, k.row, k.layer))

  implicit object Boundable extends Boundable[VoxelKey] {
    def minBound(a: VoxelKey, b: VoxelKey) = {
      VoxelKey(math.min(a.col, b.col), math.min(a.row, b.row), math.min(a.layer, b.layer))
    }

    def maxBound(a: VoxelKey, b: VoxelKey) = {
      VoxelKey(math.max(a.col, b.col), math.max(a.row, b.row), math.max(a.layer, b.layer))
    }
  }

  /** JSON Conversion */
  implicit object VoxelKeyFormat extends RootJsonFormat[VoxelKey] {
    def write(k: VoxelKey) = {
      JsObject(
        "col" -> JsNumber(k.col),
        "row" -> JsNumber(k.row),
        "layer" -> JsNumber(k.layer)
      )
    }

    def read(value: JsValue) = {
      value.asJsObject.getFields("col", "row", "layer") match {
        case Seq(JsNumber(x), JsNumber(y), JsNumber(z)) => VoxelKey(x.toInt, y.toInt, z.toInt)
        case _ => throw new DeserializationException("VoxelKey expected.")
      }
    }
  }

  /** Since [[VoxelKey]] has x and y coordinates, it can take advantage of
    * the [[SpatialComponent]] lens. Lenses are essentially "getters and setters"
    * that can be used in highly generic code.
    */
  implicit val spatialComponent = {
    Component[VoxelKey, SpatialKey](
      /* "get" a SpatialKey from VoxelKey */
      k => SpatialKey(k.col, k.row),
      /* "set" (x,y) spatial elements of a VoxelKey */
      (k, sk) => VoxelKey(sk.col, sk.row, k.layer)
    )
  }

  implicit val depthComponent = {
    Component[VoxelKey, DepthKey](
      k => DepthKey(k.layer),
      (k, dk) => VoxelKey(k.col, k.row, dk.depth)
    )
  }
}

// /** A [[KeyIndex]] based on [[VoxelKey]]. */
// class ZVoxelKeyIndex(val keyBounds: KeyBounds[VoxelKey]) extends KeyIndex[VoxelKey] {
//   /* ''Z3'' here is a convenient shorthand for any 3-dimensional key. */
//   private def toZ(k: VoxelKey): Z3 = Z3(k.x, k.y, k.z)

//   def toIndex(k: VoxelKey): BigInt = toZ(k).z

//   def indexRanges(keyRange: (VoxelKey, VoxelKey)): Seq[(BigInt, BigInt)] =
//     Z3.zranges(toZ(keyRange._1), toZ(keyRange._2))
// }

// /** A [[JsonFormat]] for [[ZVoxelKeyIndex]]. */
// class ZVoxelKeyIndexFormat extends RootJsonFormat[ZVoxelKeyIndex] {
//   val TYPE_NAME = "voxel"

//   def write(index: ZVoxelKeyIndex): JsValue = {
//     JsObject(
//       "type" -> JsString(TYPE_NAME),
//       "properties" -> JsObject("keyBounds" -> index.keyBounds.toJson)
//     )
//   }

//   def read(value: JsValue): ZVoxelKeyIndex = {
//     value.asJsObject.getFields("type", "properties") match {
//       case Seq(JsString(typeName), props) if typeName == TYPE_NAME => {
//         props.asJsObject.getFields("keyBounds") match {
//           case Seq(kb) => new ZVoxelKeyIndex(kb.convertTo[KeyBounds[VoxelKey]])
//           case _ => throw new DeserializationException("Couldn't parse KeyBounds")
//         }
//       }
//       case _ => throw new DeserializationException("Wrong KeyIndex type: ZVoxelKeyIndex expected.")
//     }
//   }
// }

// /** Register this JsonFormat with Geotrellis's central registrator.
//   * For more information on why this is necessary, see ''ShardingKeyIndex.scala''.
//   */
// class ZVoxelKeyIndexRegistrator extends KeyIndexRegistrator {
//   implicit val voxelFormat = new ZVoxelKeyIndexFormat()

//   def register(r: KeyIndexRegistry): Unit = {
//     r.register(
//       KeyIndexFormatEntry[VoxelKey, ZVoxelKeyIndex](voxelFormat.TYPE_NAME)
//     )
//   }
// }
