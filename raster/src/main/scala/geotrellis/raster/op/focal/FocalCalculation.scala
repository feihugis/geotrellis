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

package geotrellis.raster.op.focal

import geotrellis.raster._

/**
 * Declares that implementers have a result
 */
trait Resulting[T] {
  def result: T
}
/** Trait defining the ability to initialize the focal calculation with a raster. */
trait Initialization {
}

/**
 * A calculation that a FocalStrategy uses to complete
 * a focal operation.
 */
abstract class FocalCalculation[T](
    val r: Tile, n: Neighborhood, analysisArea: Option[GridBounds])
  extends Resulting[T]
{
  val bounds: GridBounds = analysisArea.getOrElse(GridBounds(r))

  def execute(): T
}

/**
 * A focal calculation that uses the Cursor focal strategy.
 */
abstract class CursorCalculation[T](
    r: Tile, n: Neighborhood, val analysisArea: Option[GridBounds])
  extends FocalCalculation[T](r, n, analysisArea)
{
  def traversalStrategy: Option[TraversalStrategy] = None
  def execute(): T = {
    CursorStrategy.execute( r, n, this, traversalStrategy, bounds)
    result
  }
  
  def calc(r: Tile, cur: Cursor): Unit
}

/**
 * A focal calculation that uses the Cellwise focal strategy
 */
abstract class CellwiseCalculation[T] (
    r: Tile, n: Neighborhood, analysisArea: Option[GridBounds])
  extends FocalCalculation[T](r, n, analysisArea)
{
  def traversalStrategy: Option[TraversalStrategy] = None

  def execute(): T = n match {
    case s: Square =>
      CellwiseStrategy.execute(r, s, this, traversalStrategy, bounds)
      result
    case _ => sys.error("Cannot use cellwise calculation with this traversal strategy.")
  }

  def add(r: Tile, x: Int, y: Int)
  def remove(r: Tile, x: Int, y: Int)
  def reset(): Unit
  def setValue(x: Int, y: Int)
}

/*
 * Trait defining the ability to initialize the focal calculation
 * with a range of variables.
 */



/*
 * Mixin's that define common raster-result functionality
 * for FocalCalculations.
 * Access the resulting raster's array tile through the 
 * 'tile' member.
 */

/**
 * Defines a focal calculation as returning
 * a [[Tile]] with [[BitArrayTile]], and defines
 * the [[Initialization]].init function for setting up the tile.
 */
trait BitArrayTileResult extends Initialization with Resulting[Tile] { self: FocalCalculation[Tile] =>
  /** [[BitArrayTile]] that will be returned by the focal calculation */
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val tile: BitArrayTile = BitArrayTile.empty(cols, rows)

//  def init(r: Tile) = {
//    cols = r.cols
//    rows = r.rows
//    tile = BitArrayTile.empty(cols, rows)
//  }

  def result = tile
}

/**
 * Defines a focal calculation as returning
 * a [[Tile]] with [[ByteArrayTile]], and defines
 * the [[Initialization]].init function for setting up the tile.
 */
trait ByteArrayTileResult extends Initialization with Resulting[Tile] { self: FocalCalculation[Tile] =>
  /** [[ByteArrayTile]] that will be returned by the focal calculation */
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val tile: ByteArrayTile = ByteArrayTile.empty(cols, rows)

//  def init(r: Tile) = {
//    cols = r.cols
//    rows = r.rows
//    tile = ByteArrayTile.empty(cols, rows)
//  }

  def result = tile
}

/**
 * Defines a focal calculation as returning
 * a [[Tile]] with [[ShortArrayTile]], and defines
 * the [[Initialization]].init function for setting up the tile.
 */
trait ShortArrayTileResult extends Initialization with Resulting[Tile] { self: FocalCalculation[Tile] =>
  /** [[ShortArrayTile]] that will be returned by the focal calculation */
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val tile: ShortArrayTile = ShortArrayTile(Array.ofDim[Short](cols * rows), cols, rows)

//  def init(r: Tile) = {
//    cols = r.cols
//    rows = r.rows
//    tile = ShortArrayTile.fill(0, cols, rows)
//  }

  def result = tile
}

/**
 * Defines a focal calculation as returning
 * a [[Tile]] with [[IntArrayTile]], and defines
 * the [[Initialization]].init function for setting up the tile.
 */
trait IntArrayTileResult extends Initialization with Resulting[Tile] { self: FocalCalculation[Tile] =>
  /** [[IntArrayTile]] that will be returned by the focal calculation */
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val tile: IntArrayTile = IntArrayTile.empty(cols, rows)

//  def init(r: Tile) = {
//    cols = r.cols
//    rows = r.rows
//    tile = IntArrayTile.empty(cols, rows)
//  }

  def result = tile
}

/**
 * Defines a focal calculation as returning
 * a [[Tile]] with [[FloatArrayTile]], and defines
 * the [[Initialization]].init function for setting up the tile.
 */
trait FloatArrayTileResult extends Initialization with Resulting[Tile] { self: FocalCalculation[Tile] =>
  /** [[FloatArrayTile]] that will be returned by the focal calculation */
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val tile: FloatArrayTile = FloatArrayTile.empty(cols, rows)

//  def init(r: Tile) = {
//    cols = r.cols
//    rows = r.rows
//    tile = FloatArrayTile.empty(cols, rows)
//  }

  def result = tile
}

/**
 * Defines a focal calculation as returning
 * a [[Tile]] with [[DoubleArrayTile]], and defines
 * the [[Initialization]].init function for setting up the tile.
 */
trait DoubleArrayTileResult extends Initialization with Resulting[Tile] { self: FocalCalculation[Tile] =>
  /** [[DoubleArrayTile]] that will be returned by the focal calculation */
  val cols: Int = bounds.width
  val rows: Int = bounds.height
  val tile: DoubleArrayTile = DoubleArrayTile.empty(cols, rows)

//  def init(r: Tile) = {
//    cols = r.cols
//    rows = r.rows
//    tile = DoubleArrayTile.empty(cols, rows)
//  }

  def result = tile
}
