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
class IDWRasterSourceBench {
  val catalogPath = "../pointcloud/src/test/resources/red-rocks"

  /** This bench benchmarks the actual PDAL reads, not the RasterSource initialization time. */
  val tin = TINRasterSource(catalogPath, overviewStrategy = Auto(6))
  val idw = IDWRasterSource(catalogPath, overviewStrategy = Auto(6))

  /**
    * jmh:run -i 10 -wi 5 -f1 -t1 .*IDWRasterSourceBench.*
    *
    * [info] Benchmark                                    Mode  Cnt     Score      Error  Units
    * [info] IDWRasterSourceBench.IDWRasterSourceReadAll  avgt   10  7923.583 ± 2071.873  ms/op
    * [info] IDWRasterSourceBench.TINRasterSourceReadAll  avgt   10   734.133 ±  237.725  ms/op
    */

  @Benchmark
  def TINRasterSourceReadAll(): Option[Raster[MultibandTile]] = tin.read()

  @Benchmark
  def IDWRasterSourceReadAll(): Option[Raster[MultibandTile]] = idw.read()

}
