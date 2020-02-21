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
