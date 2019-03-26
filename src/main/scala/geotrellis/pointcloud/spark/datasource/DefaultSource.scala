package geotrellis.pointcloud.spark.datasource

import io.pdal.pipeline._
import cats.syntax.either._
import io.circe._
import io.circe.parser._


import geotrellis.pointcloud.spark.io.hadoop.HadoopPointCloudRDD.{Options => HadoopOptions}
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index.ZCurveKeyIndexMethod

import org.apache.spark.annotation.Experimental
import org.apache.spark.sql._
import org.apache.spark.sql.sources._
import java.net.URI

import scala.util.Try

/**
  * DataSource over a GeoTrellis layer store.
  */
@Experimental
class DefaultSource extends DataSourceRegister with RelationProvider with DataSourceOptions {
  def shortName(): String = DefaultSource.SHORT_NAME

  /**
    * Create a GeoTrellis pointcloud data source.
    * @param sqlContext spark stuff
    * @param parameters required parameters are:
    *                   `path` - layer store URI (e.g. "s3://bucket/gt_layers;
    *                   `pipeline`- json pipeline string to make PDAL work;
    */
  def createRelation(sqlContext: SQLContext, parameters: Map[String, String]): BaseRelation = {
    require(parameters.contains(PATH_PARAM), s"'$PATH_PARAM' parameter is required.")

    val path     = parameters(PATH_PARAM)
    val pipeline = parameters.get(PIPELINE_PARAM).map(str => parse(str).getOrElse(Json.Null).as[PipelineExpr].valueOr(throw _)).getOrElse(Read("local"))

    new PointCloudRelation(sqlContext, path, HadoopOptions.DEFAULT.copy(pipeline = pipeline))
  }
}

object DefaultSource {
  final val SHORT_NAME = "geotrellis-pointcloud"
}
