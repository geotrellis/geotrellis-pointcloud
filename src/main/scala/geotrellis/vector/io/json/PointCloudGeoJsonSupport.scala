/*
 * Copyright 2017 Azavea
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

package geotrellis.vector.io.json

/** A trait providing automatic to and from JSON marshalling/unmarshalling using spray-json implicits.
  * @note parameter for writing json and will attempt to attach it to
  *       Feature/Geometry json representations.
  */
trait PointCloudGeoJsonSupport extends PointCloudJtsGeometryFormats

object PointCloudGeoJsonSupport extends PointCloudGeoJsonSupport
