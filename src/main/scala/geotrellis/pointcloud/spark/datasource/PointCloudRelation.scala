package geotrellis.pointcloud.spark.datasource

import geotrellis.pointcloud.spark.{Extent3D, ProjectedExtent3D}
import io.circe._
import io.circe.syntax._
import io.circe.parser._
import geotrellis.pointcloud.spark.io.hadoop._
import geotrellis.pointcloud.spark.io.hadoop.HadoopPointCloudRDD.{Options => HadoopOptions}
import geotrellis.pointcloud.spark.io.s3._
import io.pdal._
import io.pdal.pipeline._
import io.circe.syntax._
import org.apache.hadoop.fs.Path
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.{CatalystTypeConverters, InternalRow}
import org.apache.spark.sql.sources.{BaseRelation, Filter, PrunedFilteredScan, PrunedScan, TableScan}
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Row, SQLContext, SparkSession}
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder

import scala.collection.JavaConverters._

// This class has to be serializable since it is shipped over the network.
class PointCloudRelation(
  val sqlContext: SQLContext,
  path: String,
  options: HadoopOptions
) extends BaseRelation with TableScan/* with PrunedScan*/ /*with PrunedFilteredScan*/ with Serializable {

  @transient implicit lazy val sc: SparkContext = sqlContext.sparkContext

  case class PointCloudLocal(bytes: Array[Byte], dimTypes: Map[String, SizedDimType])

  object PointCloud {
    def fromJavaPointCloud(pc: io.pdal.PointCloud): PointCloudLocal =
      PointCloudLocal(pc.bytes, pc.dimTypes.asScala.toMap)
  }

  lazy val isS3: Boolean = path.startsWith("s3")

  override def schema: StructType = {
    val pointCloudHeaderSchema =
      if(isS3) ExpressionEncoder[S3PointCloudHeader]().schema else ExpressionEncoder[HadoopPointCloudHeader]().schema

    val localPipeline =
      options.pipeline.asJson
        .hcursor
        .downField("pipeline").downArray
        .downField("filename").withFocus(_ => path.asJson)
        .top.fold(options.pipeline.asJson)(identity)

    val pl = Pipeline(localPipeline.noSpaces)
    pl.execute()
    val pointCloud = try { pl.getPointViews().next().getPointCloud(0) } finally pl.dispose()


    val projectedExtent3DSchema = new StructType(
      Array[StructField](
        StructField("extend3D", ExpressionEncoder[Extent3D]().schema, nullable = false),
        StructField("crs", DataTypes.StringType, nullable = false)
      )
    )

    new StructType(
      Array[StructField](
        StructField("projectedExtent3D", projectedExtent3DSchema, nullable = false),
        StructField("points", DataTypes.createArrayType(pointCloud.deriveSchema), nullable = false)
      )
    )
  }

  /*val conf = new SparkConf().setAppName("spark-custom-datasource")
  val spark = SparkSession.builder().config(conf).master("local").getOrCreate()

  val df =
    spark.read.format("geotrellis.pointcloud.spark.datasource").option("path", "/Users/daunnc/subversions/git/github/pdal-java/examples/pdal-jni/data/1.2-with-color.las").load("/Users/daunnc/subversions/git/github/pdal-java/examples/pdal-jni/data/1.2-with-color.las")*/

  /**
    *
    *  val df =
    *  spark.read.format("geotrellis.pointcloud.spark.datasource")
    *  .option("path", "/Users/daunnc/subversions/git/github/pdal-java/examples/pdal-jni/data/1.2-with-color.las")
    *  .option("pipeline", "{"pipeline":[{"filename":"local"}]}")
    *  .load("/Users/daunnc/subversions/git/github/pdal-java/examples/pdal-jni/data/1.2-with-color.las")
    *
    * */
  override def buildScan(): RDD[Row] = {
    val rdd = if(isS3) throw new Exception("Unsupported operation") else HadoopPointCloudRDD(new Path(path), options)
    rdd.map { case (k, v) =>
      val explode = v.flatMap { pc => pc.readAll.toList.map { k => Row(k :_*) } }
      Row.fromTuple(Row(k.projectedExtent3D.extent3d, k.projectedExtent3D.crs.toProj4String) -> explode)
    }
  }

  /*override def buildScan(requiredColumns: Array[String]): RDD[Row] = {
    println("Selecting only required columns...")
    // An example, does not provide any specific performance benefits
    val initialRdd = sqlContext.sparkContext.wholeTextFiles(path).map(_._2)
    val schemaFields = schema.fields

    val rowsRdd = initialRdd.map(fileContent => {
      val lines = fileContent.split("\n")
      val data = lines.map(line => line.split("\\$").toSeq)

      val records = data.map(words => words.zipWithIndex.map {
        case (value, index) =>
          val field = schemaFields(index)
          if (requiredColumns.contains(field.name)) Some(cast(value, field.dataType)) else None
      })

      records
        .map(record => record.filter(_.isDefined))
        .map(record => Row.fromSeq(record))
    })

    rowsRdd.flatMap(row => row)
  }*/


  /*override def buildScan(requiredColumns: Array[String], filters: Array[Filter]): RDD[Row] = {
    // Nothing is actually pushed down, just iterate through all filters and print them
    println("Trying to push down filters...")
    filters foreach println
    buildScan(requiredColumns)
  }*/

  /*private def cast(value: String, dataType: DataType) = dataType match {
    case StringType => value
    case IntegerType => value.toInt
  }*/
}
