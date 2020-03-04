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

package geotrellis.pointcloud.spark.store.s3

import software.amazon.awssdk.auth.credentials.{AwsBasicCredentials, StaticCredentialsProvider}
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.regions.Region

import java.net.URI

object MockS3Client extends Serializable {
  def apply(): S3Client = {
    val cred = AwsBasicCredentials.create("minio", "password")
    val credProvider = StaticCredentialsProvider.create(cred)
    S3Client.builder()
      .endpointOverride(new URI("http://localhost:9091"))
      .credentialsProvider(credProvider)
      .region(Region.US_EAST_1)
      .build()
  }

  @transient lazy val instance: S3Client = apply()
}
