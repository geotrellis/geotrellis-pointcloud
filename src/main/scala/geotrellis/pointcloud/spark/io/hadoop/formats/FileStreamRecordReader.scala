/*
 * Copyright 2018 Azavea
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

package geotrellis.pointcloud.spark.io.hadoop.formats

import geotrellis.spark.io.hadoop._
import org.apache.hadoop.fs._
import org.apache.hadoop.io.compress.CompressionCodecFactory
import org.apache.hadoop.mapreduce._
import org.apache.hadoop.mapreduce.lib.input._

import java.io.InputStream

class FileStreamRecordReader[K, V](read: InputStream => (K, V)) extends RecordReader[K, V] {
  private var tup: (K, V) = null
  private var hasNext: Boolean = true

  def initialize(split: InputSplit, context: TaskAttemptContext) = {
    val path = split.asInstanceOf[FileSplit].getPath()
    val conf = context.getConfiguration()
    val fs = path.getFileSystem(conf)

    val is: InputStream = {
      val factory = new CompressionCodecFactory(conf)
      val codec = factory.getCodec(path)

      if (codec == null) fs.open(path)
      else codec.createInputStream(fs.open(path))
    }

    tup = read(is)
  }

  def close = {}
  def getCurrentKey = tup._1
  def getCurrentValue = { hasNext = false ; tup._2 }
  def getProgress = 1
  def nextKeyValue = hasNext
}

trait FileStreamInputFormat[K, V] extends FileInputFormat[K, V] {
  def read(is: InputStream, context: TaskAttemptContext): (K, V)

  override def isSplitable(context: JobContext, fileName: Path) = false

  override def createRecordReader(split: InputSplit, context: TaskAttemptContext): RecordReader[K, V] =
    new FileStreamRecordReader({ is => read(is, context) })
}

