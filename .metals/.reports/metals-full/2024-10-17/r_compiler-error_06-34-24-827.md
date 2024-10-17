file://<WORKSPACE>/src/main/scala/Main.scala
### file%3A%2F%2F%2FUsers%2Fhalakhalifa%2FDesktop%2FbigData%2FSparkRepo%2Fsrc%2Fmain%2Fscala%2FMain.scala:109: error: `end of file` expected but `}` found
    }
    ^

occurred in the presentation compiler.

presentation compiler configuration:
Scala version: 2.12.19
Classpath:
<HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.12.19/scala-library-2.12.19.jar [exists ]
Options:



action parameters:
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

      }
    }
    if (documentLists.isEmpty || documentLists.exists(_.isEmpty)) {
      println("No documents found for the query.")
    } else {
      val commonDocs = documentLists.reduce(_.intersect(_)).sorted // if there are multiple query terms, compute the intersection of the docs

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

    engine.saveInvertedIndexToMongoDB(outputFilePath)
    engine.queryMongoDB("play")
    engine.queryMongoDB("play soccer")
    
    engine.close()
  }
}

```



#### Error stacktrace:

```
scala.meta.internal.parsers.Reporter.syntaxError(Reporter.scala:16)
	scala.meta.internal.parsers.Reporter.syntaxError$(Reporter.scala:16)
	scala.meta.internal.parsers.Reporter$$anon$1.syntaxError(Reporter.scala:22)
	scala.meta.internal.parsers.Reporter.syntaxError(Reporter.scala:17)
	scala.meta.internal.parsers.Reporter.syntaxError$(Reporter.scala:17)
	scala.meta.internal.parsers.Reporter$$anon$1.syntaxError(Reporter.scala:22)
	scala.meta.internal.parsers.ScalametaParser.scala$meta$internal$parsers$ScalametaParser$$expectAt(ScalametaParser.scala:396)
	scala.meta.internal.parsers.ScalametaParser.scala$meta$internal$parsers$ScalametaParser$$expectAt(ScalametaParser.scala:400)
	scala.meta.internal.parsers.ScalametaParser.expect(ScalametaParser.scala:402)
	scala.meta.internal.parsers.ScalametaParser.accept(ScalametaParser.scala:418)
	scala.meta.internal.parsers.ScalametaParser.parseRuleAfterBOF(ScalametaParser.scala:62)
	scala.meta.internal.parsers.ScalametaParser.parseRule(ScalametaParser.scala:53)
	scala.meta.internal.parsers.ScalametaParser.parseSource(ScalametaParser.scala:116)
	scala.meta.parsers.Parse$.$anonfun$parseSource$1(Parse.scala:30)
	scala.meta.parsers.Parse$$anon$1.apply(Parse.scala:37)
	scala.meta.parsers.Api$XtensionParseDialectInput.parse(Api.scala:22)
	scala.meta.internal.semanticdb.scalac.ParseOps$XtensionCompilationUnitSource.toSource(ParseOps.scala:15)
	scala.meta.internal.semanticdb.scalac.TextDocumentOps$XtensionCompilationUnitDocument.toTextDocument(TextDocumentOps.scala:161)
	scala.meta.internal.pc.SemanticdbTextDocumentProvider.textDocument(SemanticdbTextDocumentProvider.scala:54)
	scala.meta.internal.pc.ScalaPresentationCompiler.$anonfun$semanticdbTextDocument$1(ScalaPresentationCompiler.scala:469)
```
#### Short summary: 

file%3A%2F%2F%2FUsers%2Fhalakhalifa%2FDesktop%2FbigData%2FSparkRepo%2Fsrc%2Fmain%2Fscala%2FMain.scala:109: error: `end of file` expected but `}` found
    }
    ^