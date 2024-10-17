import org.apache.log4j.BasicConfigurator
import org.apache.log4j.varia.NullAppender
import org.apache.spark.sql.SparkSession
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, SparkConf}
import java.io.{File, PrintWriter}
import org.mongodb.scala._
import org.mongodb.scala.model._
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.collection.JavaConverters._ 
import scala.io.Source

class SearchEngine1(documentsPath: String, mongoUri: String, dbName: String, collectionName: String) {

  // Initialize Spark context and session
  val conf = new SparkConf().setAppName("SearchEngineApp").setMaster("local[*]")
  val sc = new SparkContext(conf)
  val spark = SparkSession.builder().config(conf).getOrCreate()

  // MongoDB client and collection
  val mongoClient: MongoClient = MongoClient(mongoUri)
  val database: MongoDatabase = mongoClient.getDatabase(dbName)
  val collection: MongoCollection[Document] = database.getCollection(collectionName)

  // Function 1: Read and clean documents
  def readAndCleanDocuments(): RDD[(String, String)] = {
    val rdd = sc.wholeTextFiles(documentsPath)
    rdd.map { case (docPath, content) =>  // clean document content and associate with document names
      val docName = new File(docPath).getName // get the last part of the path
      val cleanedContent = content
        .replaceAll("[^a-zA-Z\\s]", "") // remove unwanted characters
        .replaceAll("""'+""", "") // remove apostrophes
        .toLowerCase
        .replaceAll("\\s+", " ")
        .trim

      (docName, cleanedContent)
    }
  }

  // Function 2: Build the inverted index
  def buildInvertedIndex(outputFilePath: String, cleanedRDD: RDD[(String, String)]): RDD[(String, (Int, List[String]))] = {

    val wordsWithDoc = cleanedRDD.flatMap { case (docName, cleanedContent) =>
      cleanedContent.split(" ").filter(_.nonEmpty).map(word => (word, docName))
    }
    val groupedWords = wordsWithDoc
      .distinct() // ensure (word, docName) pairs are unique
      .groupByKey()

    // create the final inverted index (word, (count of documents, list of documents))
    val invertedIndex = groupedWords.mapValues { docNames =>
      val uniqueSortedDocs = docNames.toList.distinct.sorted
      (uniqueSortedDocs.size, uniqueSortedDocs)
    }

    // the inverted index => file
    val writer = new PrintWriter(new File(outputFilePath))
    try {
      invertedIndex.sortByKey().collect().foreach { case (word, (count, docList)) =>
        writer.write(s"$word, $count, ${docList.mkString(", ")}\n")
      }
    } finally {
      writer.close()
    }

    invertedIndex
  }

  // Function 3: Save inverted index to MongoDB using bulk insert
  def saveInvertedIndexToMongoDB(filePath: String, batchSize: Int = 3000): Unit = {
    val source = Source.fromFile(filePath)
    val lines = source.getLines()

    // Collect documents to be inserted
    var batch = List[Document]()

    lines.foreach { line =>
      val parts = line.split(",\\s+")
      if (parts.length >= 3) {
        val word = parts(0)
        val count = parts(1).toInt
        val docList = parts.drop(2).toList

        val doc = Document("word" -> word, "count" -> count, "documents" -> docList)
        batch = doc :: batch

        // If the batch size is reached, insert the batch and clear it
        if (batch.size >= batchSize) {
          val insertObservable = collection.insertMany(batch.reverse).toFuture()
          Await.result(insertObservable, Duration.Inf)
          println(s"Inserted a batch of ${batch.size} documents")
          batch = List()
        }
      }
    }
    // Insert any remaining documents in the batch
    if (batch.nonEmpty) {
      val insertObservable = collection.insertMany(batch.reverse).toFuture()
      Await.result(insertObservable, Duration.Inf)
      println(s"Inserted final batch of ${batch.size} documents")
    }

    source.close()
  }
  // Function 4: Query processing to search MongoDB
// Function 4: Query processing to search MongoDB
def queryMongoDB(query: String): List[String] = {
  // Split the query into words
  val queryWords = query.toLowerCase.split("\\s+").filter(_.nonEmpty)

  // Find the documents that contain each word
  val queryResults: List[Set[String]] = queryWords.toList.map { word =>
    val docListObservable = collection.find(Document("word" -> word)).toFuture()
    val docList = Await.result(docListObservable, Duration.Inf)

    // Ensure to retrieve documents as a Set[String] to avoid duplicates
    docList.flatMap(doc => 
      doc.get("documents").map(_.asArray().getValues.asScala.toList.map(_.asString().getValue).toSet).getOrElse(Set.empty[String])
    ).toSet // Convert the List to Set to ensure uniqueness
  }

  // Intersect the sets of documents to find those that match all words in the query
  if (queryResults.nonEmpty) {
    queryResults.reduceLeft((a, b) => a.intersect(b)).toList // This gives a List[String]
  } else {
    List.empty[String]
  }
}
def close(): Unit = {
        spark.stop()
        sc.stop()
        mongoClient.close()
    }
}

object SearchEngineApp1 {
  def main(args: Array[String]): Unit = {
    val nullAppender = new NullAppender
    BasicConfigurator.configure(nullAppender)

    val mongoUri = "mongodb://localhost:27017"
    val dbName = "search_engine_db"
    val collectionName = "dictionary"

    val documentsPath = "src/main/resources/documents/archive"
    val outputFilePath = "src/main/resources/wholeInvertedIndex.txt"

    val engine = new SearchEngine1(documentsPath, mongoUri, dbName, collectionName)
    val cleanedRDD = engine.readAndCleanDocuments()
    val invertedIndexRDD = engine.buildInvertedIndex(outputFilePath, cleanedRDD)

    engine.saveInvertedIndexToMongoDB(outputFilePath)

    // Example query
    val userQuery = "play soccer"
    val results = engine.queryMongoDB(userQuery)

    println(s"Documents matching query '$userQuery': ${results.mkString(", ")}")

    engine.close()
  }
}