/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.vector

import com.vividsolutions.jts.{geom => jts}
import com.vividsolutions.jts.geom.TopologyException
import GeomFactory._
import geotrellis.proj4.CRS

trait Geometry {

  def jtsGeom: jts.Geometry

  def isValid: Boolean =
    jtsGeom.isValid

  def distance(other: Geometry): Double =
    jtsGeom.distance(other.jtsGeom)

  def withinDistance(other: Geometry, dist: Double): Boolean =
    jtsGeom.isWithinDistance(other.jtsGeom, dist)

  def centroid: PointOrNoResult =
    jtsGeom.getCentroid

  def interiorPoint: PointOrNoResult =
    jtsGeom.getInteriorPoint

  def envelope: Extent =
    if(jtsGeom.isEmpty) Extent(0.0, 0.0, 0.0, 0.0)
    else jtsGeom.getEnvelopeInternal

  def &(g: Geometry): TwoDimensionsTwoDimensionsIntersectionResult =
    intersection(g)
  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Polygon and g.
   */
  def intersection(g: Geometry): TwoDimensionsTwoDimensionsIntersectionResult =
    jtsGeom.intersection(g.jtsGeom)
  /**
   * Computes a Result that represents a Geometry made up of the points shared
   * by this Polygon and g. If it fails, it reduces the precision to avoid [[TopologyException]].
   */
  def safeIntersection(g: Geometry): TwoDimensionsTwoDimensionsIntersectionResult =
    try intersection(g)
    catch {
      case _: TopologyException => simplifier.reduce(jtsGeom).intersection(simplifier.reduce(g.jtsGeom))
    }

  override
  def equals(other: Any): Boolean =
    other match {
      case g: Geometry => jtsGeom.equals(g.jtsGeom)
      case _ => false
  }

  override
  def hashCode(): Int = jtsGeom.hashCode

  override def toString = jtsGeom.toString
}

object Geometry {
  /**
   * Wraps JTS Geometry in correct container and attempts to cast.
   * Useful when sourcing objects from JTS interface.
   */
  def apply[G <: Geometry](obj: jts.Geometry): G = {
    obj match {
      case obj: jts.Point => Point(obj)
      case obj: jts.LineString => Line(obj)
      case obj: jts.Polygon => Polygon(obj)
      case obj: jts.MultiPoint => MultiPoint(obj)
      case obj: jts.MultiLineString => MultiLine(obj)
      case obj: jts.MultiPolygon => MultiPolygon(obj)
      case obj: jts.GeometryCollection => GeometryCollection(obj)
    }
  }.asInstanceOf[G]
}

trait Relatable { self: Geometry =>

  def intersects(other: Geometry): Boolean =
    jtsGeom.intersects(other.jtsGeom)

  def disjoint(other: Geometry): Boolean =
    jtsGeom.disjoint(other.jtsGeom)

}
