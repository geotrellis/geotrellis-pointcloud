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

import geotrellis.raster.{MultibandTile, Raster}
import org.openjdk.jmh.annotations._
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
class DEMRasterSourceBench {
  val catalogPath = "../pointcloud/src/test/resources/red-rocks"

  var rs: DEMRasterSource = _
  var gtrs: GeoTrellisDEMRasterSource = _

  /**
    * jmh:run -i 3 -wi 1 -f1 -t1 .*DEMRasterSourceBench.*
    *
    * 03/06/2020 #1
    * [info] Benchmark                                                      Mode  Cnt        Score         Error  Units
    * [info] DEMRasterSourceBench.DEMRasterSourceReadAll                    avgt    3  1289156.313 ± 3962647.278  us/op
    * [info] DEMRasterSourceBench.GeoTrellisDEMRasterSourceReadAll          avgt    3  2727125.104 ± 4561499.533  us/op
    * // this is a test to move PDALTrianglesRasterzer into a separate object
    * [info] DEMRasterSourceBench.TriangulationSplitDEMRasterSourceReadAll  avgt    3  1236215.715 ± 1164712.450  us/op
    *
    * 03/06/2020 #2
    * [info] Benchmark                                              Mode  Cnt        Score         Error  Units
    * [info] DEMRasterSourceBench.DEMRasterSourceReadAll            avgt    3  1196822.191 ± 1135355.015  us/op
    * [info] DEMRasterSourceBench.GeoTrellisDEMRasterSourceReadAll  avgt    3  3209836.953 ± 8172952.538  us/op
    */

  @Setup(Level.Invocation)
  def setupData(): Unit = {
    rs   = DEMRasterSource(catalogPath)
    gtrs = GeoTrellisDEMRasterSource(catalogPath)
  }

  @Benchmark
  def DEMRasterSourceReadAll(): Option[Raster[MultibandTile]] = {
    rs.read()
  }

  @Benchmark
  def GeoTrellisDEMRasterSourceReadAll(): Option[Raster[MultibandTile]] = {
    gtrs.read()
  }
}