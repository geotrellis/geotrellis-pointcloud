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

package geotrellis.pointcloud.spark.store.s3

import geotrellis.spark.store.s3.BaseS3RecordReader

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import java.io.InputStream

/** This reader will fetch bytes of each key one at a time using [AmazonS3Client.getObject].
  * Subclass must extend [read] method to map from S3 object bytes to (K,V) */
abstract class S3StreamRecordReader[K, V](s3Client: S3Client) extends BaseS3RecordReader[K, V](s3Client: S3Client) {
  def readObjectRequest(objectRequest: GetObjectRequest): (K, V) = {
    val response = s3Client.getObject(objectRequest)
    read(objectRequest.key, response)
  }

  def read(key: String, is: InputStream): (K, V)
}
