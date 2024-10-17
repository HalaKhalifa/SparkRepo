file://<WORKSPACE>/src/main/scala/HW1/SearchEngine.scala
### file%3A%2F%2F%2FUsers%2Fhalakhalifa%2FDesktop%2FbigData%2FSparkRepo%2Fsrc%2Fmain%2Fscala%2FHW1%2FSearchEngine.scala:1: error: illegal start of definition `identifier`
package src.mainscala/HW1
                     ^

occurred in the presentation compiler.

presentation compiler configuration:
Scala version: 2.12.19
Classpath:
<HOME>/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.12.19/scala-library-2.12.19.jar [exists ]
Options:



action parameters:
uri: file://<WORKSPACE>/src/main/scala/HW1/SearchEngine.scala
text:
```scala
package src.mainscala/HW1

import org.apache.spark.sql.{SparkSession, DataFrame}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext,SparkConf}

class SearchEngine(documentsPath: String) { //, mongoUri: String

  // Initialize Spark context and session
  val conf = new SparkConf().setAppName("SearchEngineApp").setMaster("local[*]")
    //.set("spark.mongodb.output.uri", mongoUri)

  val sc = new SparkContext(conf)
  val spark = SparkSession.builder().config(conf).getOrCreate()

  // Function 1: Read and clean documents
  def readAndCleanDocuments(): RDD[String] = {
    val rdd = sc.textFile(documentsPath)
    // Clean text (removing non-alphanumeric characters but preserving apostrophes)
    rdd.map(line => 
      line.replaceAll("[^a-zA-Z\\s']", "")
          .toLowerCase()
    ).filter(_.trim.nonEmpty)
  }

  // Function 2: Build the inverted index (returns RDD)
  def buildInvertedIndex(cleanedRDD: RDD[String]): RDD[(String, List[Int])] = {
    cleanedRDD.zipWithIndex // Attach an index (doc ID) to each line of text
      .flatMap { case (line, docId) =>
        line.split("\\s+").map(word => (word, docId.toInt)) // Split words and map each to a doc ID
      }
      .groupByKey() // Group by words, aggregating all doc IDs where the word appears
      .mapValues(_.toList.distinct) // Ensure unique doc IDs per word
  }

  // Function 3: Save the inverted index to MongoDB
  def saveToMongoDB(invertedIndexRDD: RDD[(String, List[Int])]): Unit = {
    val indexDF = spark.createDataFrame(invertedIndexRDD) // Convert RDD to DataFrame for MongoDB
    //MongoSpark.write(indexDF).option("collection", "invertedIndex").mode("overwrite").save()
  }

  // Function 4: Query MongoDB for search
  def queryProcessing(query: String): Unit = {
    //val df = MongoSpark.load(spark).option("collection", "invertedIndex").load()
    //val result = df.filter(df("word") === query).show() // Display query result from MongoDB
  }

  // Close Spark session (when finished)
  def close(): Unit = {
    spark.stop()
    sc.stop()
  }
}

object SearchEngineApp {
  def main(args: Array[String]): Unit = {
    // Define the documents path and MongoDB URI
    val documentsPath = "src/main/resources/documents/archive"
    //val mongoUri = "mongodb://localhost:27017/mydatabase"

    // Create an instance of SearchEngine
    val engine = new SearchEngine(documentsPath) //, mongoUri

    // Read, clean, and process documents
    val cleanedRDD = engine.readAndCleanDocuments()
    
    // Build the inverted index
    //val invertedIndexRDD = engine.buildInvertedIndex(cleanedRDD)
    
    // Save the inverted index to MongoDB
    //engine.saveToMongoDB(invertedIndexRDD)

    // Process a query (as an example)
    //engine.queryProcessing("example_query")

    // Close the engine (shuts down Spark context/session)
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
	scala.meta.internal.parsers.ScalametaParser.statSeqBuf(ScalametaParser.scala:4109)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$statSeq$1(ScalametaParser.scala:4096)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$statSeq$1$adapted(ScalametaParser.scala:4096)
	scala.meta.internal.parsers.ScalametaParser.scala$meta$internal$parsers$ScalametaParser$$listBy(ScalametaParser.scala:562)
	scala.meta.internal.parsers.ScalametaParser.statSeq(ScalametaParser.scala:4096)
	scala.meta.internal.parsers.ScalametaParser.bracelessPackageStats$1(ScalametaParser.scala:4285)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$source$1(ScalametaParser.scala:4288)
	scala.meta.internal.parsers.ScalametaParser.atPos(ScalametaParser.scala:325)
	scala.meta.internal.parsers.ScalametaParser.autoPos(ScalametaParser.scala:369)
	scala.meta.internal.parsers.ScalametaParser.source(ScalametaParser.scala:4264)
	scala.meta.internal.parsers.ScalametaParser.entrypointSource(ScalametaParser.scala:4291)
	scala.meta.internal.parsers.ScalametaParser.parseSourceImpl(ScalametaParser.scala:119)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$parseSource$1(ScalametaParser.scala:116)
	scala.meta.internal.parsers.ScalametaParser.parseRuleAfterBOF(ScalametaParser.scala:58)
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

file%3A%2F%2F%2FUsers%2Fhalakhalifa%2FDesktop%2FbigData%2FSparkRepo%2Fsrc%2Fmain%2Fscala%2FHW1%2FSearchEngine.scala:1: error: illegal start of definition `identifier`
package src.mainscala/HW1
                     ^