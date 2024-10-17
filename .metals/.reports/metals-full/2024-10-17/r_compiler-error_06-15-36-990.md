file://<WORKSPACE>/src/main/scala/Main.scala
### java.lang.NullPointerException: Cannot invoke "scala.reflect.internal.Symbols$Symbol.isImplicit()" because the return value of "scala.reflect.internal.Trees$Tree.symbol()" is null

occurred in the presentation compiler.

presentation compiler configuration:
Scala version: 2.12.19
Classpath:
<HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.12.19/scala-library-2.12.19.jar [exists ]
Options:



action parameters:
offset: 4860
uri: file://<WORKSPACE>/src/main/scala/Main.scala
text:
```scala
import org.apache.log4j.BasicConfigurator
import org.apache.log4j.varia.NullAppender
import org.apache.spark.sql.SparkSession
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, SparkConf}
import java.io.{File, PrintWriter}
import org.mongodb.scala._
import org.mongodb.scala.model._
import com.mongodb.spark.MongoSpark
import scala.concurrent.Await
import scala.concurrent.duration.Duration
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

  def queryMongoDB(query: String): Unit = {
    // Split the query into individual terms
    val queryTerms = query.toLowerCase.split(" ").filter(_.nonEmpty)

    // For each query term, retrieve the associated document list from MongoDB
    val documentLists = queryTerms.map { term =>
      // MongoDB query to find the term in the 'dictionary' collection
      val futureResult = collection.find(Document("word" -> term)).first().toFuture()
      val result = Await.result(futureResult, Duration.Inf)

      // Extract the document list from the result (if found)
      if (result != null && result.contains("documents")) {
        result.getList("documents", classOf).toList
      } else {
        // If the term is not found, return an empty list
        List[String]()
      }
    }
    if (documentLists.isEmpty || documentLists.exists(_.isEmpty)) {
      println("No documents found for the query.")
    } else {
      val @@commonDocs = documentLists.reduce(_.intersect(_)).sorted

      // Print the result
      if (commonDocs.isEmpty) {
        println("No documents found containing all query terms.")
      } else {
        println(s"Documents containing the query terms: ${commonDocs.mkString(", ")}")
      }
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

    // Print the first 10 words and their counts and document lists
    invertedIndexRDD.take(10).foreach { case (word, (count, docList)) =>
      println(s"Word: $word, Count: $count, Documents: ${docList.mkString(", ")}")
    }

    engine.saveInvertedIndexToMongoDB(outputFilePath)

    engine.close()
  }
}

```



#### Error stacktrace:

```
scala.meta.internal.pc.PcCollector.isCorrectPos(PcCollector.scala:93)
	scala.meta.internal.pc.PcCollector.isCorrectPos$(PcCollector.scala:92)
	scala.meta.internal.pc.WithSymbolSearchCollector.isCorrectPos(PcCollector.scala:345)
	scala.meta.internal.pc.PcCollector.traverseWithParent$1(PcCollector.scala:126)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$1(PcCollector.scala:104)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$7(PcCollector.scala:167)
	scala.collection.LinearSeqOptimized.foldLeft(LinearSeqOptimized.scala:126)
	scala.collection.LinearSeqOptimized.foldLeft$(LinearSeqOptimized.scala:122)
	scala.collection.immutable.List.foldLeft(List.scala:91)
	scala.meta.internal.pc.PcCollector.traverseWithParent$1(PcCollector.scala:167)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$1(PcCollector.scala:104)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$24(PcCollector.scala:311)
	scala.collection.LinearSeqOptimized.foldLeft(LinearSeqOptimized.scala:126)
	scala.collection.LinearSeqOptimized.foldLeft$(LinearSeqOptimized.scala:122)
	scala.collection.immutable.List.foldLeft(List.scala:91)
	scala.meta.internal.pc.PcCollector.traverseWithParent$1(PcCollector.scala:311)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$1(PcCollector.scala:104)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$24(PcCollector.scala:311)
	scala.collection.LinearSeqOptimized.foldLeft(LinearSeqOptimized.scala:126)
	scala.collection.LinearSeqOptimized.foldLeft$(LinearSeqOptimized.scala:122)
	scala.collection.immutable.List.foldLeft(List.scala:91)
	scala.meta.internal.pc.PcCollector.traverseWithParent$1(PcCollector.scala:311)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$1(PcCollector.scala:104)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$24(PcCollector.scala:311)
	scala.collection.LinearSeqOptimized.foldLeft(LinearSeqOptimized.scala:126)
	scala.collection.LinearSeqOptimized.foldLeft$(LinearSeqOptimized.scala:122)
	scala.collection.immutable.List.foldLeft(List.scala:91)
	scala.meta.internal.pc.PcCollector.traverseWithParent$1(PcCollector.scala:311)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$1(PcCollector.scala:104)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$24(PcCollector.scala:311)
	scala.collection.LinearSeqOptimized.foldLeft(LinearSeqOptimized.scala:126)
	scala.collection.LinearSeqOptimized.foldLeft$(LinearSeqOptimized.scala:122)
	scala.collection.immutable.List.foldLeft(List.scala:91)
	scala.meta.internal.pc.PcCollector.traverseWithParent$1(PcCollector.scala:311)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$1(PcCollector.scala:104)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$24(PcCollector.scala:311)
	scala.collection.LinearSeqOptimized.foldLeft(LinearSeqOptimized.scala:126)
	scala.collection.LinearSeqOptimized.foldLeft$(LinearSeqOptimized.scala:122)
	scala.collection.immutable.List.foldLeft(List.scala:91)
	scala.meta.internal.pc.PcCollector.traverseWithParent$1(PcCollector.scala:311)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$1(PcCollector.scala:104)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$24(PcCollector.scala:311)
	scala.collection.LinearSeqOptimized.foldLeft(LinearSeqOptimized.scala:126)
	scala.collection.LinearSeqOptimized.foldLeft$(LinearSeqOptimized.scala:122)
	scala.collection.immutable.List.foldLeft(List.scala:91)
	scala.meta.internal.pc.PcCollector.traverseWithParent$1(PcCollector.scala:311)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$1(PcCollector.scala:104)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$15(PcCollector.scala:251)
	scala.collection.LinearSeqOptimized.foldLeft(LinearSeqOptimized.scala:126)
	scala.collection.LinearSeqOptimized.foldLeft$(LinearSeqOptimized.scala:122)
	scala.collection.immutable.List.foldLeft(List.scala:91)
	scala.meta.internal.pc.PcCollector.traverseWithParent$1(PcCollector.scala:251)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$1(PcCollector.scala:104)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$24(PcCollector.scala:311)
	scala.collection.LinearSeqOptimized.foldLeft(LinearSeqOptimized.scala:126)
	scala.collection.LinearSeqOptimized.foldLeft$(LinearSeqOptimized.scala:122)
	scala.collection.immutable.List.foldLeft(List.scala:91)
	scala.meta.internal.pc.PcCollector.traverseWithParent$1(PcCollector.scala:311)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$1(PcCollector.scala:104)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$15(PcCollector.scala:251)
	scala.collection.LinearSeqOptimized.foldLeft(LinearSeqOptimized.scala:126)
	scala.collection.LinearSeqOptimized.foldLeft$(LinearSeqOptimized.scala:122)
	scala.collection.immutable.List.foldLeft(List.scala:91)
	scala.meta.internal.pc.PcCollector.traverseWithParent$1(PcCollector.scala:251)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$1(PcCollector.scala:104)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$24(PcCollector.scala:311)
	scala.collection.LinearSeqOptimized.foldLeft(LinearSeqOptimized.scala:126)
	scala.collection.LinearSeqOptimized.foldLeft$(LinearSeqOptimized.scala:122)
	scala.collection.immutable.List.foldLeft(List.scala:91)
	scala.meta.internal.pc.PcCollector.traverseWithParent$1(PcCollector.scala:311)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$1(PcCollector.scala:104)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$15(PcCollector.scala:251)
	scala.collection.LinearSeqOptimized.foldLeft(LinearSeqOptimized.scala:126)
	scala.collection.LinearSeqOptimized.foldLeft$(LinearSeqOptimized.scala:122)
	scala.collection.immutable.List.foldLeft(List.scala:91)
	scala.meta.internal.pc.PcCollector.traverseWithParent$1(PcCollector.scala:251)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$1(PcCollector.scala:104)
	scala.meta.internal.pc.PcCollector.$anonfun$traverseSought$15(PcCollector.scala:251)
	scala.collection.LinearSeqOptimized.foldLeft(LinearSeqOptimized.scala:126)
	scala.collection.LinearSeqOptimized.foldLeft$(LinearSeqOptimized.scala:122)
	scala.collection.immutable.List.foldLeft(List.scala:91)
	scala.meta.internal.pc.PcCollector.traverseWithParent$1(PcCollector.scala:251)
	scala.meta.internal.pc.PcCollector.traverseSought(PcCollector.scala:314)
	scala.meta.internal.pc.PcCollector.traverseSought$(PcCollector.scala:95)
	scala.meta.internal.pc.WithSymbolSearchCollector.traverseSought(PcCollector.scala:345)
	scala.meta.internal.pc.PcCollector.resultWithSought(PcCollector.scala:82)
	scala.meta.internal.pc.PcCollector.resultWithSought$(PcCollector.scala:17)
	scala.meta.internal.pc.WithSymbolSearchCollector.resultWithSought(PcCollector.scala:345)
	scala.meta.internal.pc.WithSymbolSearchCollector.$anonfun$result$1(PcCollector.scala:352)
	scala.Option.map(Option.scala:230)
	scala.meta.internal.pc.WithSymbolSearchCollector.result(PcCollector.scala:352)
	scala.meta.internal.pc.PcReferencesProvider.references(PcReferencesProvider.scala:39)
	scala.meta.internal.pc.PcReferencesProvider.references$(PcReferencesProvider.scala:38)
	scala.meta.internal.pc.LocalPcReferencesProvider.references(PcReferencesProvider.scala:52)
	scala.meta.internal.pc.ScalaPresentationCompiler.$anonfun$references$1(ScalaPresentationCompiler.scala:445)
```
#### Short summary: 

java.lang.NullPointerException: Cannot invoke "scala.reflect.internal.Symbols$Symbol.isImplicit()" because the return value of "scala.reflect.internal.Trees$Tree.symbol()" is null