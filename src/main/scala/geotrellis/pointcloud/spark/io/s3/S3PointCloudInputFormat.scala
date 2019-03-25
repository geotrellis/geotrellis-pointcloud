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

package geotrellis.pointcloud.spark.io.s3

import geotrellis.spark.io.s3._
import geotrellis.pointcloud.spark.io.hadoop.formats._
import geotrellis.pointcloud.util.Filesystem

import io.pdal._
import io.circe.Json
import io.circe.syntax._
import cats.syntax.either._
import org.apache.hadoop.mapreduce.{InputSplit, TaskAttemptContext}
import org.apache.commons.io.FileUtils

import java.io.{File, InputStream}
import java.net.URI

import scala.collection.JavaConversions._

/** Process files from the path through PDAL, and reads all files point data as an Array[Byte] **/
class S3PointCloudInputFormat extends S3InputFormat[S3PointCloudHeader, List[PointCloud]] {
  def executePipeline(context: TaskAttemptContext)(key: String, pipelineJson: Json): (S3PointCloudHeader, List[PointCloud]) = {
    val dimTypeStrings: Option[Array[String]] = PointCloudInputFormat.getDimTypes(context)
    val pipeline = Pipeline(pipelineJson.noSpaces)

    // PDAL itself is not threadsafe
    AnyRef.synchronized {
      pipeline.execute
    }

    val header =
      S3PointCloudHeader(
        key,
        pipeline.getMetadata(),
        pipeline.getSchema()
      )

    // If a filter extent is set, don't actually load points.
    val (pointViewIterator, disposeIterator): (Iterator[PointView], () => Unit) =
      PointCloudInputFormat.getFilterExtent(context) match {
        case Some(filterExtent) =>
          if (header.extent3D.toExtent.intersects(filterExtent)) {
            val pvi = pipeline.getPointViews()
            (pvi, pvi.dispose _)
          } else {
            (Iterator.empty, () => ())
          }
        case None =>
          val pvi = pipeline.getPointViews()
          (pvi, pvi.dispose _)
      }


    // conversion to list to load everything into JVM memory
    val pointClouds = pointViewIterator.toList.map { pointView =>
      val pointCloud =
        dimTypeStrings match {
          case Some(ss) =>
            pointView.getPointCloud(dims = ss.map(pointView.findDimType))
          case None =>
            pointView.getPointCloud()
        }

      pointView.dispose()
      pointCloud
    }

    val result = (header, pointClouds)

    disposeIterator()
    pipeline.dispose()

    result
  }

  def createRecordReader(split: InputSplit, context: TaskAttemptContext) = {
    val s3Client = getS3Client(context)
    val pipeline = PointCloudInputFormat.getPipeline(context)

    val mode =
      pipeline
        .hcursor
        .downField("pipeline").downArray
        .downField("filename").focus.flatMap(_.as[String].toOption).getOrElse("local")

    /** PDAL can pull files directly from S3 */
    mode match {
      case "s3" =>
        new S3URIRecordReader[S3PointCloudHeader, List[PointCloud]](s3Client) {
          def read(key: String, uri: URI): (S3PointCloudHeader, List[PointCloud]) = {
            val s3Pipeline =
              pipeline
                .hcursor
                .downField("pipeline").downArray
                .downField("filename").withFocus(_ => uri.toString.asJson)
                .top.fold(pipeline)(identity)

            executePipeline(context)(key, s3Pipeline)
          }
        }

      case _ =>
        val tmpDir = {
          val dir = PointCloudInputFormat.getTmpDir(context)
          if (dir == null) Filesystem.createDirectory()
          else Filesystem.createDirectory(dir)
        }

        new S3StreamRecordReader[S3PointCloudHeader, List[PointCloud]](s3Client) {
          def read(key: String, is: InputStream): (S3PointCloudHeader, List[PointCloud]) = {
            // copy remote file into local tmp dir
            tmpDir.mkdirs() // to be sure that dirs created
            val localPath = new File(tmpDir, key.replace("/", "_"))
            FileUtils.copyInputStreamToFile(is, localPath)
            is.close()

            // use local filename path if it's present in json
            val localPipeline =
              pipeline
                .hcursor
                .downField("pipeline").downArray
                .downField("filename").withFocus(_ => localPath.getAbsolutePath.asJson)
                .top.fold(pipeline)(identity)

            try executePipeline(context)(key, localPipeline) finally {
              localPath.delete()
              tmpDir.delete()
            }
          }
        }
    }
  }
}
