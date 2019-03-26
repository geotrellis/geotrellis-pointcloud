package geotrellis.pointcloud.spark.datasource

trait DataSourceOptions {
  final val PATH_PARAM = "path"
  final val PIPELINE_PARAM = "pipeline"
}

object DataSourceOptions extends DataSourceOptions