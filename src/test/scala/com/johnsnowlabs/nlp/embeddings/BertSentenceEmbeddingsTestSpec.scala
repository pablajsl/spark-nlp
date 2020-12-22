package com.johnsnowlabs.nlp.embeddings

import com.johnsnowlabs.nlp.Chunk2Doc
import com.johnsnowlabs.nlp.annotator.SentenceDetectorDLModel
import com.johnsnowlabs.nlp.annotators.sbd.pragmatic.SentenceDetector
import com.johnsnowlabs.nlp.base.DocumentAssembler
import com.johnsnowlabs.nlp.util.io.ResourceHelper
import org.apache.spark.ml.Pipeline
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.functions._
import org.scalatest._
import com.johnsnowlabs.nlp.annotator._
import scala.io.Source
import scala.collection.mutable

class BertSentenceEmbeddingsTestSpec extends FlatSpec {

  "BertSentenceEmbeddings" should "not produce duplicate sentences" in {
    val text = Source.fromFile("/data/sample3.txt").getLines().mkString("\n")
    val documentAssembler = new DocumentAssembler().setInputCol("text").setOutputCol("document")
    val sentenceDetector = new SentenceDetector().setInputCols(Array("document")).setOutputCol("sentence")
    val tokenizer = new Tokenizer().setInputCols(Array("sentence")).setOutputCol("token")
    val word_embeddings = WordEmbeddingsModel
      .pretrained("embeddings_clinical", "en", "clinical/models")
      .setInputCols(Array("sentence", "token"))
      .setOutputCol("embeddings")
    val clinical_ner = NerDLModel
      .pretrained("ner_clinical_large", "en", "clinical/models")
      .setInputCols(Array("sentence", "token", "embeddings"))
      .setOutputCol("ner")
    val ner_converter = new NerConverter()
      .setInputCols(Array("sentence", "token", "ner"))
      .setOutputCol("ner_chunk")
    val c2doc = new Chunk2Doc().setInputCols("ner_chunk").setOutputCol("ner_chunk_doc")
    val sbert_embedder = BertSentenceEmbeddings
      .pretrained("sbiobert_base_cased_mli","en","clinical/models")
      .setInputCols(Array("ner_chunk_doc"))
      .setOutputCol("sbert_embeddings")
    val sbert_pipeline_snomed = new Pipeline().setStages(
      Array(
        documentAssembler,
        sentenceDetector,
        tokenizer,
        word_embeddings,
        clinical_ner,
        ner_converter,
        c2doc,
        sbert_embedder))
    val data_ner = ResourceHelper.spark.createDataFrame(Seq(
      (1, text.toString)
    )).toDF("id", "text")
    val sbert_models = sbert_pipeline_snomed.fit(data_ner)
    val sbert_emb = sbert_models.transform(data_ner)

//    sbert_emb.select(explode(col("ner_chunk_doc.result"))).show()

    sbert_emb.select(
      explode(
        arrays_zip(
          col("ner_chunk_doc.result"),
          col("ner_chunk_doc.begin"),
          col("ner_chunk_doc.end"))).alias("res"))
      .select(
        expr("res['0']").alias("chunk"),
        expr("res['1']").alias("begin"),
        expr("res['2']").alias("end"))
      .orderBy(col("begin"))
      .show()

    sbert_emb.select(
      explode(
        arrays_zip(
          col("sbert_embeddings.result"),
          col("sbert_embeddings.begin"),
          col("sbert_embeddings.end"))).alias("res"))
      .select(
        expr("res['0']").alias("chunk"),
        expr("res['1']").alias("begin"),
        expr("res['2']").alias("end"))
      .orderBy(col("begin"))
      .show()
  }
  "BertSentenceEmbeddings" should "produce consistent embeddings" in {

    val testData = ResourceHelper.spark.createDataFrame(Seq(

      (1, "John loves apples."),
      (2, "Mary loves oranges. John loves Mary.")

    )).toDF("id", "text")

    val document = new DocumentAssembler()
      .setInputCol("text")
      .setOutputCol("document")

    val sentence = SentenceDetectorDLModel.pretrained()
      .setInputCols("document")
      .setOutputCol("sentence")

    val embeddings = BertSentenceEmbeddings
      .load("/models/sbert/s-biobert_base_cased_mli")
      //.pretrained("sent_small_bert_L2_128")
      .setInputCols(Array("sentence"))
      .setOutputCol("bert")
      .setMaxSentenceLength(32)

    val pipeline = new Pipeline().setStages(Array(document, sentence, embeddings))

    val model = pipeline.fit(testData)
    val results = model.transform(testData).select("id", "bert").collect()

    results.foreach(row => {
      val rowI = row.get(0)
      row.get(1).asInstanceOf[mutable.WrappedArray[GenericRowWithSchema]].zipWithIndex.foreach(t => {
        print("%1$-3s\t%2$-3s\t%3$-30s\t".format(rowI.toString, t._2.toString, t._1.getString(3)))
        println(t._1.get(5).asInstanceOf[mutable.WrappedArray[Float]].slice(0, 5).map("%1$-7.3f".format(_)).mkString(" "))
      })
    })

    model.stages(2).asInstanceOf[BertSentenceEmbeddings].write.overwrite().save("./tmp_bert_sentence_embed")
  }

  "BertSentenceEmbeddings" should "correctly work with empty tokens" in {

    val testData = ResourceHelper.spark.createDataFrame(Seq(
      (1, "This is my first sentence. This is my second."),
      (2, "This is my third sentence. This is my forth.")
    )).toDF("id", "text")

    val document = new DocumentAssembler()
      .setInputCol("text")
      .setOutputCol("document")

    val sentence = new SentenceDetector()
      .setInputCols("document")
      .setOutputCol("sentence")

    val embeddings = BertSentenceEmbeddings.pretrained("sent_small_bert_L2_128")
      .setInputCols("sentence")
      .setOutputCol("bert")
      .setCaseSensitive(false)
      .setMaxSentenceLength(32)


    val pipeline = new Pipeline().setStages(Array(document, sentence, embeddings))

    val pipelineDF = pipeline.fit(testData).transform(testData)

    pipelineDF.select("bert.embeddings").show()

  }

}
