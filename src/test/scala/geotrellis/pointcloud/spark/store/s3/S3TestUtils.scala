package geotrellis.pointcloud.spark.store.s3

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model._

import scala.collection.JavaConverters._

object S3TestUtils {
  def cleanBucket(client: S3Client, bucket: String) = {
    try {
      val listObjectsReq =
        ListObjectsV2Request.builder()
          .bucket(bucket)
          .build()
      val objIdentifiers = client.listObjectsV2Paginator(listObjectsReq)
        .contents
        .asScala
        .map { s3obj => ObjectIdentifier.builder.key(s3obj.key).build() }
        .toList
      val deleteDefinition = Delete.builder()
        .objects(objIdentifiers:_*)
        .build()
      val deleteReq = DeleteObjectsRequest.builder()
        .bucket(bucket)
        .delete(deleteDefinition)
        .build()
      client.deleteObjects(deleteReq)
    } catch {
      case nsb: NoSuchBucketException =>
        val createBucketReq =
          CreateBucketRequest.builder()
            .bucket(bucket)
            .build()
        client.createBucket(createBucketReq)
    }
  }
}
