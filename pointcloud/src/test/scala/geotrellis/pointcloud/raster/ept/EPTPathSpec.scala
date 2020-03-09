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

package geotrellis.pointcloud.raster.ept

import org.scalatest.FunSpec

class EPTPathSpec extends FunSpec {
  describe("EPTPathSpec") {
    it("relative path") {
      assert(EPTPath.parse("data/my-data.tiff").value == "data/my-data.tiff")
    }

    it("relative path with a scheme") {
      assert(EPTPath.parse("ept://data/my-data.tiff").value == "data/my-data.tiff")
    }

    it("relative path and localfs in a scheme") {
      assert(EPTPath.parse("ept+file://data/my-data.tiff").value == "file://data/my-data.tiff")
    }

    it("absolute path and s3 in a scheme") {
      assert(EPTPath.parse("ept+s3://data/my-data.tiff").value == "s3://data/my-data.tiff")
    }

    it("absolute path with a scheme") {
      assert(EPTPath.parse("ept+file:///tmp/data.tiff").value == "file:///tmp/data.tiff")
    }
  }
}
