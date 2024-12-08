package SparkNLP

import org.apache.spark.sql.SparkSession
import com.johnsnowlabs.nlp.base._
import com.johnsnowlabs.nlp.annotators._
import com.johnsnowlabs.nlp.annotators.pos.perceptron.PerceptronModel
import com.johnsnowlabs.nlp.annotators.ner.crf.NerCrfModel
import com.johnsnowlabs.nlp.embeddings.WordEmbeddingsModel
import org.apache.spark.ml.Pipeline
import org.apache.spark.sql.functions._


object POSvsNERApp {
  def main(args: Array[String]): Unit = {

    // Step 1: Set up Spark NLP with SparkSession
    val spark = SparkSession.builder()
      .appName("SparkNLP")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    // Step 2: Read The Dataset
    val dataPath = "src/main/resources/spark_nlp_dataset.parquet"
    val df = spark.read.parquet(dataPath)

    // Step 3: Define Pipeline Stages
    // DocumentAssembler => transform the raw text data into a structured document format that Spark NLP can work with.
    val documentAssembler = new DocumentAssembler()
      .setInputCol("text")
      .setOutputCol("document")

    // Tokenizer => splits the text into tokens (words) based on the structure that was created by the the DocumentAssembler.
    val tokenizer = new Tokenizer()
      .setInputCols(Array("document"))
      .setOutputCol("token")
    
    // WordEmbeddingsModel => Assigns pre-trained word embeddings (GloVe) to the tokens.
    val wordEmbeddings = WordEmbeddingsModel.pretrained("glove_100d", "en")
      .setInputCols(Array("document", "token"))
      .setOutputCol("embeddings")

    // PerceptronModel => Performs POS tagging on tokens.
    val posTagger = PerceptronModel.pretrained()
      .setInputCols(Array("document", "token"))
      .setOutputCol("pos")

    // NerCrfModel => Performs NER tagging to identify named entities (persons, organizations).
    val nerTagger = NerCrfModel.pretrained()
      .setInputCols(Array("document", "token", "embeddings", "pos"))
      .setOutputCol("ner")


    val pipeline = new Pipeline()
      .setStages(Array(documentAssembler, tokenizer, wordEmbeddings, posTagger, nerTagger))

    // Step 4: Fit and Transform Dataset
    val transformedDf = pipeline.fit(df).transform(df)

    // Step 5: Extract POS and NER Columns & Analyze Relationships
    val posNerPairs = transformedDf
      .withColumn("POS", explode($"pos.result"))   // Explode POS tags
      .withColumn("NER", explode($"ner.result")) // Explode NER tags
      .select("POS", "NER")

    // Show the raw relationshisp between POS and NER
    posNerPairs.show(truncate = false) 

    // Group by POS and NER
    val posNerRelationship = posNerPairs
      .groupBy("POS", "NER")
      .count()  // Count occurrences of each POS-NER pair
      .orderBy(desc("count"))  // Order by most frequent relationships

    posNerRelationship.show(truncate = false)

    // Filter specific relationships
    // Example1: Find the # of NNP (Proper Nouns) related to Organizations
    val NNPORGRelationship = posNerRelationship
      .filter($"POS" === "NNP" && $"NER" === "I-ORG")
      
    println(s"Proper Nouns assigned to Organizations:")
    NNPORGRelationship.show(truncate = false)

    // Example2: Find the # of JJ (Adjective) related to Organizations or Persons
    val JJORGRelationship = posNerRelationship
      .filter($"POS" === "JJ" && ($"NER" === "I-ORG" || $"NER" === "I-PER" ))
    println(s"Adjective assigned to Organizations || Person:")
    JJORGRelationship.show(truncate = false)

    // Cross-tab POS and NER where the values in the cells represent
    // 'how often each combination of POS and NER tags occurs in the dataset'.

    val posNerCrosstab = posNerPairs
      .stat
      .crosstab("POS", "NER")
    println(s"Cross-tab:")
    posNerCrosstab.show(truncate = false)

  }
}

// POS: The Part-of-Speech tag of a token => NNP (Proper Noun), JJ (Adjective), NN (Noun)
// NER: The Named Entity tag assigned to the token => I-ORG (Organization), I-PER (Person), O (Outside, i.e., not a named entity)