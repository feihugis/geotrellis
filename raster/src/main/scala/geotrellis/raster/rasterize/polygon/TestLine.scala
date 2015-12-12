package geotrellis.raster.rasterize.polygon

import geotrellis.raster.RasterExtent
import geotrellis.vector._

case class TestLine(rowMin: Int, rowMax: Int, x0: Double, y0: Double, x1: Double, y1: Double, inverseSlope: Double) {
  def horizontal: Boolean = rowMin == rowMax
  
  def intercept(y: Double) =
    x0 + (y - y0) * inverseSlope
}

case class TestLineSet(testLines: Seq[TestLine], rowMin: Int, rowMax: Int) {
  def merge(other: TestLineSet): TestLineSet =
    TestLineSet(other.testLines ++ testLines, math.min(rowMin, other.rowMin), math.max(rowMax, other.rowMax))
}

object TestLineSet {
  lazy val EMPTY = TestLineSet(Seq(), Int.MaxValue, Int.MinValue)

  def apply(line: Line, re: RasterExtent, includeExterior: Boolean = false): TestLineSet = {
    var rowMin = Int.MaxValue
    var rowMax = Int.MinValue

    val testLines = 
      line
        .points
        .sliding(2)
        .flatMap { l =>
          val p1 = l(0)
          val p2 = l(1)

          val (x0, y0, x1, y1) =
            if (p1.y < p2.y) {
              (p1.x, p1.y, p2.x, p2.y)
            } else {
              (p2.x, p2.y, p1.x, p1.y)
            }

        val (minRow, maxRow) = 
          if (includeExterior) {
            (math.floor(re.mapYToGridDouble(y1)).toInt,
             math.ceil(re.mapYToGridDouble(y0)).toInt)
          } else {
            (math.floor(re.mapYToGridDouble(y1) + 0.5).toInt,
             math.floor(re.mapYToGridDouble(y0) - 0.5).toInt)

          }

          val inverseSlope = (x1 - x0) / (y1 - y0)

          if (minRow > maxRow ||
              p1.y == p2.y ||
              inverseSlope == java.lang.Double.POSITIVE_INFINITY ||
              inverseSlope == java.lang.Double.NEGATIVE_INFINITY ) {
            // drop horizontal lines
            None
          } else {
            if(minRow < rowMin) rowMin = minRow
            if(maxRow > rowMax) rowMax = maxRow

            Some(TestLine(minRow, maxRow, x0, y0, x1, y1, inverseSlope))
          }
         }
        .toList
    
    TestLineSet(testLines, rowMin, rowMax)
  }
}
