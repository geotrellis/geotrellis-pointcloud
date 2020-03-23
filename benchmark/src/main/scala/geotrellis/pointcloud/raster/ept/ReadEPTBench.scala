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

import geotrellis.pointcloud.raster.rasterize.triangles.PDALTrianglesRasterizer

import geotrellis.raster.{MultibandTile, Raster, RasterExtent}
import geotrellis.vector.Extent
import io.pdal.{PointView, TriangularMesh}
import io.pdal.pipeline.{ENil, FilterDelaunay, ReadEpt}
import cats.syntax.option._
import org.openjdk.jmh.annotations._

import scala.collection.JavaConverters._
import java.util.concurrent.TimeUnit

@BenchmarkMode(Array(Mode.AverageTime))
@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
class ReadEPTBench {
  val catalogPath = "../pointcloud/src/test/resources/red-rocks"

  /** Params are used to match [[DEMRasterSourceBench]] behavior. **/
  val extent @ Extent(exmin, eymin, exmax, eymax) = Extent(481968.0, 4390186.0, 482856.0, 4391074.0)
  val readEpt: ReadEpt = ReadEpt(
    filename = catalogPath,
    bounds = s"([$exmin, $eymin], [$exmax, $eymax])".some,
    resolution = 6.9375.some
  )

  /**
    * jmh:run -i 10 -wi 5 -f1 -t1 .*ReadEPTBench.*
    *
    * 03/23/2020 #1
    * [info] Benchmark                          Mode  Cnt    Score    Error  Units
    * [info] ReadEPTBench.EPTRasterize          avgt   10  419.780 ± 63.084  ms/op
    * [info] ReadEPTBench.EPTRead               avgt   10  207.858 ± 18.166  ms/op
    * [info] ReadEPTBench.EPTReadTriangulation  avgt   10  388.973 ± 76.595  ms/op
    */
  @Benchmark
  def EPTReadTriangulation(): Option[TriangularMesh] = {
    val pipeline = readEpt ~ FilterDelaunay() toPipeline

    try {
      if (pipeline.validate()) {
        pipeline.execute

        pipeline
          .getPointViews()
          .asScala
          .toList
          .headOption
          .map { _.getTriangularMesh() }
      } else None
    } finally pipeline.close()
  }

  @Benchmark
  def EPTRead(): Option[PointView] = {
    val pipeline = readEpt ~ ENil toPipeline

    try {
      if (pipeline.validate()) {
        pipeline.execute

        pipeline
          .getPointViews()
          .asScala
          .toList
          .headOption
      } else None
    } finally pipeline.close()
  }

  @Benchmark
  def EPTRasterize(): Option[Raster[MultibandTile]] = {
    val pipeline = readEpt ~ FilterDelaunay() toPipeline

    try {
      if (pipeline.validate()) {
        pipeline.execute

        pipeline
          .getPointViews()
          .asScala
          .toList
          .headOption
          .map {
            PDALTrianglesRasterizer
              .native(_, RasterExtent(extent, 128, 128))
              .mapTile(MultibandTile(_))
          }
      } else None
    } finally pipeline.close()
  }
}
