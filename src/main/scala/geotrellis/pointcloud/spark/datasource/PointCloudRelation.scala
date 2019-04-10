package geotrellis.pointcloud.spark.datasource

import geotrellis.pointcloud.spark.io.hadoop._
import geotrellis.pointcloud.spark.io.hadoop.HadoopPointCloudRDD.{Options => HadoopOptions}
import geotrellis.pointcloud.util.Filesystem
import geotrellis.proj4.CRS
import geotrellis.spark.io.hadoop.HdfsUtils
import geotrellis.vector.Extent

import cats.implicits._
import io.pdal._
import io.circe.syntax._
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.sources.{BaseRelation, Filter, PrunedFilteredScan, PrunedScan, TableScan}
import org.apache.spark.sql.types._
import org.apache.spark.sql.{Row, SQLContext}

import java.io.File

import scala.collection.JavaConverters._

// This class has to be serializable since it is shipped over the network.
class PointCloudRelation(
  val sqlContext: SQLContext,
  path: String,
  options: HadoopOptions
) extends BaseRelation with TableScan/* with PrunedScan with PrunedFilteredScan*/ with Serializable {

  @transient implicit lazy val sc: SparkContext = sqlContext.sparkContext

  // TODO: switch between HadoopPointCloudRDD and S3PointcCloudRDD
  lazy val isS3: Boolean = path.startsWith("s3")

  override def schema: StructType = {
    lazy val (local, fixedPath) =
      if(path.startsWith("s3") || path.startsWith("hdfs")) {
        val tmpDir = Filesystem.createDirectory()
        val remotePath = new Path(path)
        // copy remote file into local tmp dir
        val localPath = new File(tmpDir, remotePath.getName)
        HdfsUtils.copyPath(remotePath, new Path(localPath.getAbsolutePath), sc.hadoopConfiguration)
        (true, localPath.toString)
      } else (false, path)

    val localPipeline =
      options.pipeline
        .hcursor
        .downField("pipeline").downArray
        .downField("filename").withFocus(_ => fixedPath.asJson)
        .top.fold(options.pipeline)(identity)

    val pl = Pipeline(localPipeline.noSpaces)
    if (pl.validate()) pl.execute()
    val pointCloud = try {
      pl.getPointViews().next().getPointCloud(0)
    } finally {
      pl.dispose()
      if(local) println(new File(fixedPath).delete)
    }

    val rdd = HadoopPointCloudRDD(new Path(path), options)

    val md: (Option[Extent], Option[CRS]) =
      rdd
        .map { case (header, _) => (header.projectedExtent3D.map(_.extent3d.toExtent), header.crs) }
        .reduce { case ((e1, c), (e2, _)) => ((e1, e2).mapN(_ combine _), c) }

    val metadata = new MetadataBuilder().putString("metadata", md.asJson.noSpaces).build

    pointCloud.deriveSchema(metadata)
  }

  override def buildScan(): RDD[Row] = {
    val rdd = HadoopPointCloudRDD(new Path(path), options)
    rdd.flatMap { _._2.flatMap { pc => pc.readAll.toList.map { k => Row(k: _*) } } }
  }
}
