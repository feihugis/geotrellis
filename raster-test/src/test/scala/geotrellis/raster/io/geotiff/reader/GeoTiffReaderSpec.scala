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

package geotrellis.raster.io.geotiff.reader

import geotrellis.raster._
import geotrellis.raster.io.arg._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.io.geotiff.util._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.summary.polygonal._

import geotrellis.vector.{Point, Extent}
import geotrellis.raster.testkit._
import geotrellis.proj4._

import monocle.syntax.apply._
import org.scalactic.Tolerance

import scala.io.{Source, Codec}
import scala.collection.immutable.HashMap

import java.util.BitSet
import java.nio.ByteBuffer

import spire.syntax.cfor._
import org.scalatest._

class GeoTiffReaderSpec extends FunSpec
    with Matchers
    with BeforeAndAfterAll
    with RasterMatchers
    with GeoTiffTestUtils {

  override def afterAll = purge

  describe("reading an ESRI generated Float32 geotiff with 0 NoData value") {
    it("matches an arg produced from geotrellis.gdal reader of that tif") {
      val tile = SinglebandGeoTiff.compressed(geoTiffPath("us_ext_clip_esri.tif")).convert(FloatConstantNoDataCellType)

      val expectedTile =
        ArgReader.read(geoTiffPath("us_ext_clip_esri.json")).tile

      assertEqual(tile, expectedTile)
    }

  }

  describe("reading slope.tif") {

    it("should match the ARG version") {
      val path = "slope.tif"
      val argPath = s"$baseDataPath/data/slope.json"

      val tile = SinglebandGeoTiff.compressed(s"$baseDataPath/$path").convert(FloatConstantNoDataCellType)

      val expectedTile =
        ArgReader.read(argPath).tile

      assertEqual(tile, expectedTile)
    }

  }

  describe("reading modelTransformation.tiff") {
    val path = "modelTransformation.tiff"
    val compressed: SinglebandGeoTiff = SinglebandGeoTiff.compressed(geoTiffPath(path))
    val tile = compressed.tile
    val bounds = tile.gridBounds
    bounds.width should be (1121)

    import Tolerance._
    compressed.crs should be (CRS.fromName("EPSG:4326"))
    if(compressed.extent.min.distance(Point(59.9955397,  30.0044603))>0.0001) {
      compressed.extent.min should be (Point(59.9955397,  30.0044603))
    }

    if(compressed.extent.max.distance(Point(69.9955397,  40.0044603))>0.0001) {
      compressed.extent.max should be (Point(69.9955397,  40.0044603))
    }

  }

  describe("reading compressed file must yield same image array as uncompressed file") {

    it("must read econic_lzw.tif and match uncompressed file") {
      val decomp = SinglebandGeoTiff.compressed(geoTiffPath("econic_lzw.tif"))
      val uncomp = SinglebandGeoTiff.compressed(s"$baseDataPath/econic.tif")

      assertEqual(decomp.tile, uncomp.tile)
    }

    it("must read econic_zlib.tif and match uncompressed file") {
      val decomp = SinglebandGeoTiff.compressed(geoTiffPath("econic_zlib.tif"))
      val uncomp = SinglebandGeoTiff.compressed(s"$baseDataPath/econic.tif")

      assertEqual(decomp.tile, uncomp.tile)
    }

    it("must read econic_zlib_tiled.tif and match uncompressed file") {
      val decomp = SinglebandGeoTiff.compressed(geoTiffPath("econic_zlib_tiled.tif"))
      val uncomp = SinglebandGeoTiff.compressed(s"$baseDataPath/econic.tif")

      assertEqual(decomp.tile, uncomp.tile)
    }

    it("must read econic_zlib_tiled_bandint.tif and match uncompressed file") {
      val decomp = SinglebandGeoTiff.compressed(geoTiffPath("econic_zlib_tiled_bandint.tif"))
      val uncomp = SinglebandGeoTiff.compressed(s"$baseDataPath/econic.tif")

      assertEqual(decomp.tile, uncomp.tile)
    }

    it("must read all-ones.tif and match uncompressed file") {
      val decomp = SinglebandGeoTiff.compressed(geoTiffPath("all-ones.tif"))
      val uncomp = SinglebandGeoTiff.compressed(geoTiffPath("all-ones-no-comp.tif"))

      assertEqual(decomp.tile, uncomp.tile)
    }
  }

  describe("reading tiled file must yield same image as strip files") {

    it("must read us_ext_clip_esri.tif and match strip file") {
      val tiled = SinglebandGeoTiff.compressed(geoTiffPath("us_ext_clip_esri.tif"))
      val striped = SinglebandGeoTiff.compressed(geoTiffPath("us_ext_clip_esri_stripes.tif"))

      assertEqual(tiled.tile, striped.tile)
    }

  }

  describe("reading bit rasters") {
    it("should match bit tile the ArrayTile pulled out of the resulting GeoTiffTile") {
      val expected = SinglebandGeoTiff.compressed(geoTiffPath("uncompressed/tiled/bit.tif")).tile
      val actual = SinglebandGeoTiff.compressed(geoTiffPath("uncompressed/tiled/bit.tif")).tile.toArrayTile

      assertEqual(actual, expected)
      assertEqual(expected, actual)
    }

    it("must read bilevel_tiled.tif and match strip file") {
      val tiled = SinglebandGeoTiff.compressed(geoTiffPath("bilevel_tiled.tif"))
      val striped = SinglebandGeoTiff.compressed(geoTiffPath("bilevel.tif"))

      assertEqual(tiled.tile, striped.tile)
    }

    it("should match bit and byte-converted rasters") {
      val actual = SinglebandGeoTiff.compressed(geoTiffPath("bilevel.tif")).tile
      val expected = SinglebandGeoTiff(geoTiffPath("bilevel.tif")).tile.convert(BitCellType)

      assertEqual(actual, expected)
    }


  }

  describe("match tiff tags and geokeys correctly") {

    it("must match aspect.tif tiff tags") {
      val tiffTags = TiffTagsReader.read(s"$baseDataPath/aspect.tif")

      tiffTags.cols should equal (1500L)

      tiffTags.rows should equal (1350L)

      tiffTags.bitsPerSample should be (32)

      tiffTags.compression should equal (1)

      (tiffTags &|-> TiffTags._basicTags ^|->
        BasicTags._photometricInterp get) should equal (1)

      (tiffTags &|-> TiffTags._basicTags ^|->
        BasicTags._stripOffsets get) match {
        case Some(stripOffsets) => stripOffsets.size should equal (1350)
        case None => fail
      }

      (tiffTags &|-> TiffTags._basicTags ^|->
        BasicTags._samplesPerPixel get) should equal (1)

      (tiffTags &|-> TiffTags._basicTags ^|->
        BasicTags._rowsPerStrip get) should equal (1L)

      (tiffTags &|-> TiffTags._basicTags ^|->
        BasicTags._stripByteCounts get) match {
        case Some(stripByteCounts) => stripByteCounts.size should equal (1350)
        case None => fail
      }

      (tiffTags &|-> TiffTags._nonBasicTags ^|->
        NonBasicTags._planarConfiguration get) match {
        case Some(planarConfiguration) => planarConfiguration should equal (1)
        case None => fail
      }

      val sampleFormat =
        (tiffTags
          &|-> TiffTags._dataSampleFormatTags
          ^|-> DataSampleFormatTags._sampleFormat get)
      sampleFormat should be (3)

      (tiffTags &|-> TiffTags._geoTiffTags
        ^|-> GeoTiffTags._modelPixelScale get) match {
        case Some(modelPixelScales) => {
          modelPixelScales._1 should equal (10.0)
          modelPixelScales._2 should equal (10.0)
          modelPixelScales._3 should equal (0.0)
        }
        case None => fail
      }

      (tiffTags &|-> TiffTags._geoTiffTags
        ^|-> GeoTiffTags._modelTiePoints get) match {
        case Some(modelTiePoints) if (modelTiePoints.size == 1) => {
          val (p1, p2) = modelTiePoints(0)
          p1.x should equal (0.0)
          p1.y should equal (0.0)
          p1.z should equal (0.0)
          p2.x should equal (630000.0)
          p2.y should equal (228500.0)
          p2.z should equal (0.0)
        }
        case None => fail
      }

      (tiffTags &|-> TiffTags._geoTiffTags
        ^|-> GeoTiffTags._gdalInternalNoData get) match {
        case Some(gdalInternalNoData) => gdalInternalNoData should equal (-9999.0)
        case None => fail
      }
    }

    it("must match aspect.tif geokeys") {
      val tiffTags = TiffTagsReader.read(s"$baseDataPath/aspect.tif")

      tiffTags.tags.headTags("AREA_OR_POINT") should be ("AREA")

      val extent = tiffTags.extent

      val minX = extent.xmin should equal (630000.0)
      val minY = extent.ymin should equal (215000.0)
      val maxX = extent.xmax should equal (645000.0)
      val maxY = extent.ymax should equal (228500.0)

      tiffTags.bandType should equal (Float32BandType)
    }

  }

  /*
   The proj4 string generator matches the listgeo -proj4 <file> command.

   The listgeo command sometimes drops precision compared to our generator,
   therefore we sometimes increase the epsilon double comparison value.
   */
  describe("reads GeoTiff CRS correctly") {

    it("should read slope.tif CS correctly") {
      val crs = SinglebandGeoTiff.compressed(s"$baseDataPath/slope.tif")crs

      val correctCRS = CRS.fromString("+proj=utm +zone=10 +datum=NAD27 +units=m +no_defs")

      crs should equal(correctCRS)
    }

    it("should read aspect.tif CS correctly") {
      val crs = SinglebandGeoTiff.compressed(s"$baseDataPath/aspect.tif").crs

      val correctProj4String = "+proj=lcc +lat_1=36.16666666666666 +lat_2=34.33333333333334 +lat_0=33.75 +lon_0=-79 +x_0=609601.22 +y_0=0 +datum=NAD83 +units=m +no_defs"

      val correctCRS = CRS.fromString(correctProj4String)

      crs should equal(crs)
    }

    it("should read econic.tif CS correctly") {
      val crs = SinglebandGeoTiff.compressed(s"$baseDataPath/econic.tif").crs

      val correctProj4String = "+proj=eqdc +lat_0=33.76446202777777 +lon_0=-117.4745428888889 +lat_1=33.90363402777778 +lat_2=33.62529002777778 +x_0=0 +y_0=0 +datum=NAD27 +units=m +no_defs"

      val correctCRS = CRS.fromString(correctProj4String)

      crs should equal(correctCRS)
    }

    it("should read bilevel.tif CS correctly") {
      val crs = SinglebandGeoTiff.compressed(geoTiffPath("bilevel.tif")).crs

      val correctProj4String = "+proj=tmerc +lat_0=0 +lon_0=-3.45233333 +k=0.9996 +x_0=1500000 +y_0=0 +ellps=intl +units=m +no_defs"

      val correctCRS = CRS.fromString(correctProj4String)

      crs should equal(correctCRS)
    }

    it("should read all-ones.tif CS correctly") {
      val crs = SinglebandGeoTiff.compressed(geoTiffPath("all-ones.tif")).crs

      val correctCRS = CRS.fromString("+proj=longlat +datum=WGS84 +no_defs")

      crs should equal(correctCRS)
    }

    it("should read colormap.tif CS correctly") {
      val crs = SinglebandGeoTiff.compressed(geoTiffPath("colormap.tif")).crs

      val correctCRS = CRS.fromString("+proj=longlat +datum=WGS84 +no_defs")
      crs should equal(correctCRS)
    }

    it("should read us_ext_clip_esri.tif CS correctly") {
      val crs = SinglebandGeoTiff.compressed(geoTiffPath("us_ext_clip_esri.tif")).crs

      val correctCRS = CRS.fromString("+proj=longlat +datum=WGS84 +no_defs")

      crs should equal(correctCRS)
    }

    it("should read ndvi-web-mercator.tif CS correctly") {
      val crs = SinglebandGeoTiff.compressed(geoTiffPath("ndvi-web-mercator.tif")).crs

      val correctCRS = CRS.fromString("+proj=merc +a=6378137 +b=6378137 +lat_ts=0.0 +lon_0=0.0 +x_0=0.0 +y_0=0 +k=1.0 +units=m +nadgrids=@null +wktext  +no_defs")

      crs.toProj4String should equal(correctCRS.toProj4String)
    }

    it("should read ny-state-plane.tif CS correctly") {
      val crs = SinglebandGeoTiff.compressed(geoTiffPath("ny-state-plane.tif")).crs

      val correctCRS = CRS.fromString("+proj=tmerc +lat_0=40 +lon_0=-74.33333333333333 +k=0.999966667 +x_0=152400.3048006096 +y_0=0 +datum=NAD27 +units=us-ft +no_defs ")

      crs.toProj4String should equal(correctCRS.toProj4String)
    }

    it("should read alaska-polar-3572.tif CS correctly") {
      val crs = SinglebandGeoTiff.compressed(geoTiffPath("alaska-polar-3572.tif")).crs

      val correctCRS = CRS.fromString("+proj=laea +lat_0=90 +lon_0=-150 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs ")
      crs.toProj4String should equal(correctCRS.toProj4String)
    }

  }

  describe("reads file data correctly") {

    val MeanEpsilon = 1e-8

    def testMinMaxAndMean(min: Double, max: Double, mean: Double, file: String) {
      val SinglebandGeoTiff(tile, extent, _, _, _) = SinglebandGeoTiff.compressed(s"$baseDataPath/$file")

      tile.polygonalMax(extent, extent.toPolygon) should be (max)
      tile.polygonalMin(extent, extent.toPolygon) should be (min)
      tile.polygonalMean(extent, extent.toPolygon) should be (mean +- MeanEpsilon)
    }

    it("should read UINT 16 little endian files correctly") {
      val min = 71
      val max = 237
      val mean = 210.66777801514
      val file = "reproject/nlcd_tile_wsg84.tif"

      testMinMaxAndMean(min, max, mean, file)
    }

    it("should read FLOAT 32 little endian files correctly") {
      val min = 0
      val max = 360
      val mean = 190.02287812187
      val file = "aspect.tif"

      testMinMaxAndMean(min, max, mean, file)
    }

    it("should read GeoTiff without GeoKey Directory correctly") {
      val SinglebandGeoTiff(tile, extent, crs, _, _) = SinglebandGeoTiff.compressed(geoTiffPath("no-geokey-dir.tif"))

      crs should be (LatLng)
      extent should be (Extent(307485, 3911490, 332505, 3936510))

      val (max, min, mean) = (74032, -20334, 17.023709809131)

      tile.polygonalMax(extent, extent.toPolygon) should be (max)
      tile.polygonalMin(extent, extent.toPolygon) should be (min)
      tile.polygonalMean(extent, extent.toPolygon) should be (mean +- MeanEpsilon)
    }

    it("should read GeoTiff with tags") {
      val tags = SinglebandGeoTiff.compressed(geoTiffPath("tags.tif")).tags.headTags

      tags("TILE_COL") should be ("6")
      tags("units") should be ("kg m-2 s-1")
      tags("lon#axis") should be ("X")
      tags("_FillValue") should be ("1e+20")
      tags("NC_GLOBAL#driving_model_ensemble_member") should be("r1i1p1")
    }

    it("should read GeoTiff with no extent data correctly") {
      val Raster(tile, extent) = SinglebandGeoTiff.compressed(geoTiffPath("tags.tif")).raster

      extent should be (Extent(0, 0, tile.cols, tile.rows))
    }

    it("should read GeoTiff with multiple bands correctly") {
      val mbTile  = MultibandGeoTiff(geoTiffPath("multi-tag.tif")).tile

      mbTile.bandCount should be (4)

      cfor(0)(_ < 4, _ + 1) { i =>
        val tile = mbTile.band(i)
        tile.cellType should be (UByteCellType)
        tile.dimensions should be ((500, 500))
      }
    }

    it("should read GeoTiff with bands metadata correctly") {
      val geoTiff = MultibandGeoTiff(geoTiffPath("multi-tag.tif"))

      val tags = geoTiff.tags

      tags.headTags("HEADTAG") should be ("1")
      tags.headTags("TAG_TYPE") should be ("HEAD")
      tags.headTags.size should be (3)

      val bandCount = geoTiff.tile.bandCount

      bandCount should be (4)

      cfor(0)(_ < 4, _ + 1) { i =>
        val correctMetadata = Map(
          "BANDTAG" -> (i + 1).toString,
          "TAG_TYPE" -> s"BAND${i + 1}"
        )

        tags.bandTags(i) should be (correctMetadata)
      }
    }

    it("should read GeoTiff with ZLIB compression and needs exact segment sizes") {
      val geoTiff = SinglebandGeoTiff.compressed(geoTiffPath("nex-pr-tile.tif"))

      val tile = geoTiff.tile
      cfor(0)(_ < tile.rows, _ + 1) { row =>
        cfor(0)(_ < tile.cols, _ + 1) { col =>
          isNoData(tile.get(col, row)) should be (true)
        }
      }
    }

    it("should read clipped GeoTiff with byte NODATA value") {
      // Conversions carried out for both of these; first for byte -> float, second for user defined no data to constant
      val geoTiff = SinglebandGeoTiff.compressed(geoTiffPath("nodata-tag-byte.tif")).convert(FloatConstantNoDataCellType)
      val geoTiff2 = SinglebandGeoTiff.compressed(geoTiffPath("nodata-tag-float.tif")).convert(FloatConstantNoDataCellType)
      assertEqual(geoTiff.toArrayTile, geoTiff2)
    }

    it("should read NODATA string with length = 4") {
      val geoTiff = SinglebandGeoTiff.compressed(s"$baseDataPath/sbn/SBN_inc_percap-nodata-clip.tif")
      geoTiff.tile.cellType should be (ByteConstantNoDataCellType)
    }
  }

  describe("Reading and writing special metadata tags ") {
    val temp = java.io.File.createTempFile("geotiff-writer", ".tif");
    val path = temp.getPath()

    it("must read a tif, change the pixel sample type, write it out, and then read the correct in") {
      val gt = SinglebandGeoTiff.compressed(s"$baseDataPath/slope.tif")
      var headTags = gt.tags.headTags
      assert(headTags(Tags.AREA_OR_POINT) == "AREA")
      headTags -= Tags.AREA_OR_POINT
      headTags += ((Tags.AREA_OR_POINT, "POINT"))
      val tags = Tags(headTags, gt.tags.bandTags)
      val gt2 = gt.copy(tags = tags)
      writer.GeoTiffWriter.write(gt2, path)
      addToPurge(path)
      val gt3 = SinglebandGeoTiff.compressed(path)

      gt3.tags.headTags.get(Tags.AREA_OR_POINT) should be (Some("POINT"))
    }

    it("must read a tif, set a datetime, write it out, and then read the correct in") {
      val gt = SinglebandGeoTiff.compressed(s"$baseDataPath/reproject/nlcd_tile_wsg84.tif")
      var headTags = gt.tags.headTags
      headTags += ((Tags.TIFFTAG_DATETIME, "1988:02:18 13:59:59"))
      val tags = Tags(headTags, gt.tags.bandTags)
      val gt2 = gt.copy(tags = tags)
      writer.GeoTiffWriter.write(gt2, path)
      addToPurge(path)
      val gt3 = SinglebandGeoTiff.compressed(path)
      assert(gt3.crs == LatLng)

      gt3.tags.headTags.get(Tags.TIFFTAG_DATETIME) should be (Some("1988:02:18 13:59:59"))
    }
  }

  describe("handling special CRS cases") {
    it("can handle an ESRI written GeoTiff in WebMercator") {
      val tif = SinglebandGeoTiff.compressed(s"$baseDataPath/propval_bg_01_01.tif")
      tif.crs should be (WebMercator)
    }
  }
}

class PackBitsGeoTiffReaderSpec extends FunSpec
    with RasterMatchers
    with GeoTiffTestUtils {

  describe("Reading geotiffs with PACKBITS compression") {
    it("must read a single band bit raster as a multiband") {
      val singlebandGeoTiff = SinglebandGeoTiff(geoTiffPath("deflate/striped/bit.tif"))
      val multibandGeoTiff = MultibandGeoTiff(geoTiffPath("deflate/striped/bit.tif"))
      assertEqual(multibandGeoTiff.tile.band(0), singlebandGeoTiff.tile)
    }

    it("must read econic_packbits.tif and match uncompressed file") {
      val actual = SinglebandGeoTiff.compressed(geoTiffPath("econic_packbits.tif")).tile
      val expected = SinglebandGeoTiff.compressed(s"$baseDataPath/econic.tif").tile

      assertEqual(actual, expected)
    }

    it("must read previously erroring packbits compression .tif and match uncompressed file") {
      val expected = SinglebandGeoTiff.compressed(geoTiffPath("packbits-error-uncompressed.tif")).tile
      val actual = SinglebandGeoTiff.compressed(geoTiffPath("packbits-error.tif")).tile

      assertEqual(actual, expected)
    }
  }
}
