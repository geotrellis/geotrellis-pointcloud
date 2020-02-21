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

package geotrellis.pointcloud.spark.datasource

import geotrellis.pointcloud.spark.store.hadoop.HadoopPointCloudRDD.{Options => HadoopOptions}
import io.pdal.pipeline._

import cats.syntax.either._
import io.circe._
import io.circe.parser._

import org.apache.spark.annotation.Experimental
import org.apache.spark.sql._
import org.apache.spark.sql.sources._

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
    val pipeline = parameters.get(PIPELINE_PARAM).map { str => parse(str).getOrElse(Json.Null) }.getOrElse(List(Read("local")): Json)

    new PointCloudRelation(sqlContext, path, HadoopOptions.DEFAULT.copy(pipeline = pipeline))
  }
}

object DefaultSource {
  final val SHORT_NAME = "geotrellis-pointcloud"
}
