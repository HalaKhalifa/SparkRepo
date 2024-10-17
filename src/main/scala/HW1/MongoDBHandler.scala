package src.main.scala.HW1

import org.mongodb.scala._
import org.mongodb.scala.model._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source
import scala.jdk.CollectionConverters._

object MongoDBHandler {

  def getMongoClient(mongoUri: String): MongoClient = MongoClient(mongoUri)

  def getMongoCollection(client: MongoClient, dbName: String, collectionName: String): MongoCollection[Document] = {
    client.getDatabase(dbName).getCollection(collectionName)
  }

  def saveInvertedIndex(collection: MongoCollection[Document], filePath: String, batchSize: Int): Unit = {
    val source = Source.fromFile(filePath)
    val lines = source.getLines()
    var batch = List[Document]()

    lines.foreach { line =>
      val parts = line.split(",\\s+")
      if (parts.length >= 3) {
        val word = parts(0)
        val count = parts(1).toInt
        val docList = parts.drop(2).toList
        val doc = Document("word" -> word, "count" -> count, "documents" -> docList)
        batch = doc :: batch

        if (batch.size >= batchSize) {
          val insertObservable = collection.insertMany(batch.reverse).toFuture()
          Await.result(insertObservable, Duration.Inf)
          batch = List()
        }
      }
    }

    if (batch.nonEmpty) {
      val insertObservable = collection.insertMany(batch.reverse).toFuture()
      Await.result(insertObservable, Duration.Inf)
    }

    source.close()
  }

  def query(collection: MongoCollection[Document], query: String): List[String] = {
    val queryWords = query.toLowerCase.split("\\s+").filter(_.nonEmpty)
    val queryResults = queryWords.toList.map { word =>
      val docListObservable = collection.find(Document("word" -> word)).toFuture()
      val docList = Await.result(docListObservable, Duration.Inf)
      docList.flatMap(doc => doc.get("documents").map(_.asArray().getValues.asScala.map(_.asString().getValue).toSet).getOrElse(Set.empty[String])).toSet
    }

    if (queryResults.nonEmpty) queryResults.reduceLeft(_ intersect _).toList else List.empty[String]
  }
}