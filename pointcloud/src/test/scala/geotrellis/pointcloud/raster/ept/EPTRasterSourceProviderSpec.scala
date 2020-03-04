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

import geotrellis.raster.RasterSource
import org.scalatest.FunSpec

class EPTRasterSourceProviderSpec extends FunSpec {
  describe("EPTRasterSourceProvider") {
    val provider = new EPTRasterSourceProvider()

    it("should process a local prefixed string") {
      assert(provider.canProcess("ept+file:///tmp/path/to/random"))
    }

    it("should process an s3 prefixed string") {
      assert(provider.canProcess("ept+s3://bucket/key"))
    }

    it("should produce a GeoTiffRasterSource from a string") {
      assert(RasterSource("ept+file://dumping-ground/part-2/random").isInstanceOf[DEMRasterSource])
    }
  }
}

