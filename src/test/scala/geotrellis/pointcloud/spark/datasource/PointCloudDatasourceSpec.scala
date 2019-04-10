package geotrellis.pointcloud.spark.datasource

import geotrellis.pointcloud.spark._

import org.apache.spark.sql.{DataFrame, Row}

import org.scalatest.{FunSpec, Matchers}

class PointCloudDatasourceSpec extends FunSpec with Matchers with PointCloudTestEnvironment with PointCloudSpatialTestFiles {
  override def beforeAll(): Unit = {
    super.beforeAll()
    setS3Credentials
  }

  describe("PointCloudDatasourceSpec") {
    it("should read las / laz files") {
      val las: DataFrame =
        ssc
          .read
          .format("geotrellis.pointcloud.spark.datasource")
          .option("path", s"${testResources.getAbsolutePath}/las/1.2-with-color.las")
          .load

      las.take(1).toList shouldBe List(Row(637012.24, 849028.31, 431.66, 143, 1, 1, 1, 0, 1, -9.0, -124, 7326, 245380.78254962614, 68, 77, 88))
    }

    it("should read csv files") {
      val text: DataFrame =
        ssc
          .read
          .format("geotrellis.pointcloud.spark.datasource")
          .option("path", s"${testResources.getAbsolutePath}/csv/test-pdal.csv")
          .option("pipeline", """{"pipeline":[{"filename":"","separator":",","type":"readers.text","spatialreference":"EPSG:4326"}]}""")
          .load

      text.take(1).toList shouldBe List(Row(289814.15, 4320978.61, 170.76, 2.0))
    }

    ignore("should read mbio files") {
      val all: DataFrame =
        ssc
          .read
          .format("geotrellis.pointcloud.spark.datasource")
          .option("path", s"${testResources.getAbsolutePath}/mbio/test.all")
          .option("pipeline", """{"pipeline":[{"filename":"","type":"readers.mbio","format":"MBF_EM710RAW", "spatialreference":"EPSG:4326"}]}""") // MBF_EMOLDRAW, MBF_EM710RAW
          .load

      all.take(1).toList shouldBe List(Row(8.267645357194883E-5, -1.5016648357392838E-4, -18.49906635284424, 6.0184808919234E10, -21.7f))
    }

    ignore("should read a csv file from s3 via hadoop API") {
      val text: DataFrame =
        ssc
          .read
          .format("geotrellis.pointcloud.spark.datasource")
          .option("path", "s3n://geotrellis-test/test-pdal.csv")
          .option("pipeline", """{"pipeline":[{"filename":"","separator":",","type":"readers.text","spatialreference":"EPSG:4326"}]}""")
          .load

      text.take(1).toList shouldBe List(Row(289814.15, 4320978.61, 170.76, 2.0))
    }

    ignore("should read mbio files from s3 via hadoop API") {
      val text: DataFrame =
        ssc
          .read
          .format("geotrellis.pointcloud.spark.datasource")
          .option("path", "s3n://test-bucket-mbio/test.all")
          .option("pipeline", """{"pipeline":[{"filename":"","type":"readers.mbio","format":"MBF_EM710RAW", "spatialreference":"EPSG:4326"}]}""") // MBF_EMOLDRAW, MBF_EM710RAW
          .load

      text.take(1).toList shouldBe List(Row(8.267645357194883E-5, -1.5016648357392838E-4, -18.49906635284424, 6.0184808919234E10, -21.7f))
    }
  }
}
