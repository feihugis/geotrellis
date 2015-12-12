package geotrellis.spark.io.hadoop

import geotrellis.spark.io.json._
import geotrellis.spark._
import geotrellis.spark.io.index.{KeyIndexMethod, KeyIndex}
import geotrellis.spark.io._
import org.apache.avro.Schema
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.rdd.RDD
import spray.json._
import spray.json.DefaultJsonProtocol._
import scala.reflect._

class HadoopLayerWriter[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container](
  rootPath: Path,
  val attributeStore: AttributeStore[JsonFormat],
  rddWriter: HadoopRDDWriter[K, V],
  keyIndexMethod: KeyIndexMethod[K])
(implicit val cons: ContainerConstructor[K, V, Container])
  extends Writer[LayerId, Container with RDD[(K, V)]] {

  def write(id: LayerId, rdd: Container with RDD[(K, V)]): Unit = {
    implicit val sc = rdd.sparkContext

    val layerPath = new Path(rootPath,  s"${id.name}/${id.zoom}")

    val header =
      HadoopLayerHeader(
        keyClass = classTag[K].toString(),
        valueClass = classTag[V].toString(),
        path = layerPath
      )
    val metaData = cons.getMetaData(rdd)
    val keyBounds = implicitly[Boundable[K]].getKeyBounds(rdd.asInstanceOf[RDD[(K, V)]])
    val keyIndex = keyIndexMethod.createIndex(keyBounds)

    try {
      implicit val mdFormat = cons.metaDataFormat
      attributeStore.writeLayerAttributes(id, header, metaData, keyBounds, keyIndex, Option.empty[Schema])
      // TODO: Writers need to handle Schema changes

      rddWriter.write(rdd, layerPath, keyIndex)
    } catch {
      case e: Exception => throw new LayerWriteError(id).initCause(e)
    }
  }
}

object HadoopLayerWriter {

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container[_]](
    rootPath: Path,
    attributeStore: HadoopAttributeStore,
    rddWriter: HadoopRDDWriter[K, V],
    indexMethod: KeyIndexMethod[K])
  (implicit cons: ContainerConstructor[K, V, Container[K]]): HadoopLayerWriter[K, V, Container[K]] =
    new HadoopLayerWriter (
      rootPath = rootPath,
      attributeStore = attributeStore,
      rddWriter = rddWriter,
      keyIndexMethod = indexMethod
    )

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container[_]](
    rootPath: Path,
    rddWriter: HadoopRDDWriter[K, V],
    indexMethod: KeyIndexMethod[K])
  (implicit cons: ContainerConstructor[K, V, Container[K]]): HadoopLayerWriter[K, V, Container[K]] =
    apply(
      rootPath = rootPath,
      attributeStore = HadoopAttributeStore(new Path(rootPath, "attributes"), new Configuration),
      rddWriter = rddWriter,
      indexMethod = indexMethod)

  def apply[K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container[_]](
    rootPath: Path,
    indexMethod: KeyIndexMethod[K])
  (implicit
    format: HadoopFormat[K, V],
    cons: ContainerConstructor[K, V, Container[K]]): HadoopLayerWriter[K, V, Container[K]] =
    apply(
      rootPath = rootPath,
      rddWriter = new HadoopRDDWriter[K, V](HadoopCatalogConfig.DEFAULT),
      indexMethod = indexMethod)
}