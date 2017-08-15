/*
 * Copyright 2016 Azavea
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

package geotrellis.vector.reproject

import geotrellis.proj4._
import geotrellis.vector.reproject.{PointCloudJtsReproject => JtsReproject}

import com.vividsolutions.jts.{geom => jts}

object Implicits extends Implicits

trait Implicits {
  implicit class JtsReprojectPoint(p: jts.Point) {
    def reproject(src: CRS, dest: CRS): jts.Point = JtsReproject(p, src, dest)
    def reproject(transform: Transform): jts.Point = JtsReproject(p, transform)
  }

  implicit class JtsReprojectLineString(l: jts.LineString) {
    def reproject(src: CRS, dest: CRS): jts.LineString = JtsReproject(l, src, dest)
    def reproject(transform: Transform): jts.LineString = JtsReproject(l, transform)
  }

  implicit class JtsReprojectPolygon(p: jts.Polygon) {
    def reproject(src: CRS, dest: CRS): jts.Polygon = JtsReproject(p, src, dest)
    def reproject(transform: Transform): jts.Polygon = JtsReproject(p, transform)
  }

  implicit class JtsReprojectMultiPoint(mp: jts.MultiPoint) {
    def reproject(src: CRS, dest: CRS): jts.MultiPoint = JtsReproject(mp, src, dest)
    def reproject(transform: Transform): jts.MultiPoint = JtsReproject(mp, transform)
  }

  implicit class JtsReprojectMutliLineString(ml: jts.MultiLineString) {
    def reproject(src: CRS, dest: CRS): jts.MultiLineString = JtsReproject(ml, src, dest)
    def reproject(transform: Transform): jts.MultiLineString = JtsReproject(ml, transform)
  }

  implicit class JtsReprojectMutliPolygon(mp: jts.MultiPolygon) {
    def reproject(src: CRS, dest: CRS): jts.MultiPolygon = JtsReproject(mp, src, dest)
    def reproject(transform: Transform): jts.MultiPolygon = JtsReproject(mp, transform)
  }

  implicit class JtsReprojectGeometryCollection(gc: jts.GeometryCollection) {
    def reproject(src: CRS, dest: CRS): jts.GeometryCollection = JtsReproject(gc, src, dest)
    def reproject(transform: Transform): jts.GeometryCollection = JtsReproject(gc, transform)
  }

  implicit class JtsReprojectGeometry(g: jts.Geometry) {
    def reproject(src: CRS, dest: CRS): jts.Geometry = JtsReproject(g, src, dest)
    def reproject(transform: Transform): jts.Geometry = JtsReproject(g, transform)
  }

}
