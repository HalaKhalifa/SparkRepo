file://<WORKSPACE>/src/main/scala/Main.scala
### file%3A%2F%2F%2FUsers%2Fhalakhalifa%2FDesktop%2FbigData%2FSparkRepo%2Fsrc%2Fmain%2Fscala%2FMain.scala:134: error: declaration requires a type
    val queryResult
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
    val queryResult
    
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
	scala.meta.internal.parsers.ScalametaParser.$anonfun$patDefOrDcl$7(ScalametaParser.scala:3369)
	scala.Option.fold(Option.scala:251)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$patDefOrDcl$1(ScalametaParser.scala:3369)
	scala.meta.internal.parsers.ScalametaParser.autoEndPos(ScalametaParser.scala:372)
	scala.meta.internal.parsers.ScalametaParser.autoEndPos(ScalametaParser.scala:377)
	scala.meta.internal.parsers.ScalametaParser.patDefOrDcl(ScalametaParser.scala:3346)
	scala.meta.internal.parsers.ScalametaParser.defOrDclOrSecondaryCtor(ScalametaParser.scala:3315)
	scala.meta.internal.parsers.ScalametaParser.localDef(ScalametaParser.scala:4188)
	scala.meta.internal.parsers.ScalametaParser.iter$7(ScalametaParser.scala:4225)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$blockStatSeq$1(ScalametaParser.scala:4247)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$blockStatSeq$1$adapted(ScalametaParser.scala:4197)
	scala.meta.internal.parsers.ScalametaParser.scala$meta$internal$parsers$ScalametaParser$$listBy(ScalametaParser.scala:562)
	scala.meta.internal.parsers.ScalametaParser.blockStatSeq(ScalametaParser.scala:4197)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$blockOnBrace$2(ScalametaParser.scala:2385)
	scala.meta.internal.parsers.ScalametaParser.inBracesOnOpen(ScalametaParser.scala:265)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$blockOnBrace$1(ScalametaParser.scala:2383)
	scala.meta.internal.parsers.ScalametaParser.atPos(ScalametaParser.scala:325)
	scala.meta.internal.parsers.ScalametaParser.autoPos(ScalametaParser.scala:369)
	scala.meta.internal.parsers.ScalametaParser.blockOnBrace(ScalametaParser.scala:2383)
	scala.meta.internal.parsers.ScalametaParser.blockOnBrace(ScalametaParser.scala:2385)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$blockExprOnBrace$1(ScalametaParser.scala:2388)
	scala.meta.internal.parsers.ScalametaParser.blockExprPartial(ScalametaParser.scala:2367)
	scala.meta.internal.parsers.ScalametaParser.blockExprOnBrace(ScalametaParser.scala:2387)
	scala.meta.internal.parsers.ScalametaParser.simpleExpr0(ScalametaParser.scala:2079)
	scala.meta.internal.parsers.ScalametaParser.simpleExpr(ScalametaParser.scala:2061)
	scala.meta.internal.parsers.ScalametaParser.prefixExpr(ScalametaParser.scala:2058)
	scala.meta.internal.parsers.ScalametaParser.postfixExpr(ScalametaParser.scala:1924)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$expr$2(ScalametaParser.scala:1552)
	scala.meta.internal.parsers.ScalametaParser.atPosOpt(ScalametaParser.scala:327)
	scala.meta.internal.parsers.ScalametaParser.autoPosOpt(ScalametaParser.scala:370)
	scala.meta.internal.parsers.ScalametaParser.expr(ScalametaParser.scala:1480)
	scala.meta.internal.parsers.ScalametaParser.expr(ScalametaParser.scala:1381)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$funDefRest$1(ScalametaParser.scala:3547)
	scala.meta.internal.parsers.ScalametaParser.autoEndPos(ScalametaParser.scala:372)
	scala.meta.internal.parsers.ScalametaParser.autoEndPos(ScalametaParser.scala:377)
	scala.meta.internal.parsers.ScalametaParser.funDefRest(ScalametaParser.scala:3511)
	scala.meta.internal.parsers.ScalametaParser.funDefOrDclOrExtensionOrSecondaryCtor(ScalametaParser.scala:3460)
	scala.meta.internal.parsers.ScalametaParser.defOrDclOrSecondaryCtor(ScalametaParser.scala:3320)
	scala.meta.internal.parsers.ScalametaParser.nonLocalDefOrDcl(ScalametaParser.scala:3299)
	scala.meta.internal.parsers.ScalametaParser$$anonfun$templateStat$1.applyOrElse(ScalametaParser.scala:4150)
	scala.meta.internal.parsers.ScalametaParser$$anonfun$templateStat$1.applyOrElse(ScalametaParser.scala:4147)
	scala.PartialFunction.$anonfun$runWith$1$adapted(PartialFunction.scala:145)
	scala.meta.internal.parsers.ScalametaParser.statSeqBuf(ScalametaParser.scala:4107)
	scala.meta.internal.parsers.ScalametaParser.getStats$2(ScalametaParser.scala:4137)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$scala$meta$internal$parsers$ScalametaParser$$templateStatSeq$3(ScalametaParser.scala:4138)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$scala$meta$internal$parsers$ScalametaParser$$templateStatSeq$3$adapted(ScalametaParser.scala:4136)
	scala.meta.internal.parsers.ScalametaParser.scala$meta$internal$parsers$ScalametaParser$$listBy(ScalametaParser.scala:562)
	scala.meta.internal.parsers.ScalametaParser.scala$meta$internal$parsers$ScalametaParser$$templateStatSeq(ScalametaParser.scala:4136)
	scala.meta.internal.parsers.ScalametaParser.scala$meta$internal$parsers$ScalametaParser$$templateStatSeq(ScalametaParser.scala:4128)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$templateBody$1(ScalametaParser.scala:4006)
	scala.meta.internal.parsers.ScalametaParser.inBracesOr(ScalametaParser.scala:260)
	scala.meta.internal.parsers.ScalametaParser.inBraces(ScalametaParser.scala:256)
	scala.meta.internal.parsers.ScalametaParser.templateBody(ScalametaParser.scala:4006)
	scala.meta.internal.parsers.ScalametaParser.templateBodyOpt(ScalametaParser.scala:4009)
	scala.meta.internal.parsers.ScalametaParser.templateAfterExtends(ScalametaParser.scala:3960)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$templateOpt$1(ScalametaParser.scala:4001)
	scala.meta.internal.parsers.ScalametaParser.atPos(ScalametaParser.scala:325)
	scala.meta.internal.parsers.ScalametaParser.autoPos(ScalametaParser.scala:369)
	scala.meta.internal.parsers.ScalametaParser.templateOpt(ScalametaParser.scala:3993)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$objectDef$1(ScalametaParser.scala:3722)
	scala.meta.internal.parsers.ScalametaParser.autoEndPos(ScalametaParser.scala:372)
	scala.meta.internal.parsers.ScalametaParser.autoEndPos(ScalametaParser.scala:377)
	scala.meta.internal.parsers.ScalametaParser.objectDef(ScalametaParser.scala:3714)
	scala.meta.internal.parsers.ScalametaParser.tmplDef(ScalametaParser.scala:3601)
	scala.meta.internal.parsers.ScalametaParser.topLevelTmplDef(ScalametaParser.scala:3589)
	scala.meta.internal.parsers.ScalametaParser$$anonfun$2.applyOrElse(ScalametaParser.scala:4121)
	scala.meta.internal.parsers.ScalametaParser$$anonfun$2.applyOrElse(ScalametaParser.scala:4115)
	scala.PartialFunction.$anonfun$runWith$1$adapted(PartialFunction.scala:145)
	scala.meta.internal.parsers.ScalametaParser.statSeqBuf(ScalametaParser.scala:4107)
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

file%3A%2F%2F%2FUsers%2Fhalakhalifa%2FDesktop%2FbigData%2FSparkRepo%2Fsrc%2Fmain%2Fscala%2FMain.scala:134: error: declaration requires a type
    val queryResult
                   ^