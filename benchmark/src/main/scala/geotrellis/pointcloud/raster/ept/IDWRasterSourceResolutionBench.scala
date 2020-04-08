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

import geotrellis.raster.{CellSize, GridExtent, MultibandTile, Raster, RasterExtent}
import geotrellis.raster.io.geotiff.Auto
import geotrellis.proj4.LatLng

import org.openjdk.jmh.annotations._
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class IDWRasterSourceResolutionBench {
  val catalogPath = "../pointcloud/src/test/resources/red-rocks"

  /** This bench benchmarks the actual PDAL reads, not the RasterSource initialization time. */
  val rs   = IDWRasterSource(catalogPath)
  val rsAuto2 = IDWRasterSource(catalogPath)

  /**
    * jmh:run -i 3 -wi 1 -f1 -t1 .*IDWRasterSourceResolutionBench.*
    *
    * jmh:run -i 10 -wi 5 -f1 -t1 .*IDWRasterSourceResolutionBench.*
    *
    */

  @Benchmark
  def IDWRasterSourceNaturalRead(): Option[Raster[MultibandTile]] = rs.read()

  @Benchmark
  def IDWRasterSourceAuto2Read(): Option[Raster[MultibandTile]] = rsAuto2.read()

}
