package com.johnsnowlabs.nlp.annotators.sentence_detector_dl

import org.apache.spark.ml.param.{BooleanParam, Params, StringArrayParam}

trait SentenceDetectorDLParams  extends Params {

  /** whether to only utilize custom bounds for sentence detection */
  val useCustomBoundsOnly = new BooleanParam(this, "useCustomBoundsOnly", "whether to only utilize custom bounds for sentence detection")


  /** characters used to explicitly mark sentence bounds */
  val customBounds: StringArrayParam = new StringArrayParam(this, "customBounds", "characters used to explicitly mark sentence bounds")

  /** Impossible penultimates
    *
    * @group param
    **/
  val impossiblePenultimates = new StringArrayParam(this, "impossiblePenultimates", "Impossible penultimates")

  /** Set impossible penultimates
    *
    * @group setParam
    **/
  def setImpossiblePenultimates(impossiblePenultimates: Array[String]):
    this.type = set(this.impossiblePenultimates, impossiblePenultimates)

  /** Get impossible penultimates
    *
    * @group getParam
    **/
  def getImpossiblePenultimates: Array[String] = $(this.impossiblePenultimates)

  /** A flag indicating whether to split sentences into different Dataset rows. Useful for higher parallelism in
    * fat rows. Defaults to false.
    *
    * @group getParam
    **/
  def explodeSentences = new BooleanParam(this, "explodeSentences", "Split sentences in separate rows")


  /** Whether to split sentences into different Dataset rows. Useful for higher parallelism in fat rows. Defaults to false.
    *
    * @group setParam
    **/
  def setExplodeSentences(value: Boolean): this.type = set(this.explodeSentences, value)


  /** Whether to split sentences into different Dataset rows. Useful for higher parallelism in fat rows. Defaults to false.
    *
    * @group getParam
    **/
  def getExplodeSentences: Boolean = $(this.explodeSentences)
  setDefault(
    impossiblePenultimates -> Array(),
    explodeSentences -> false,
    useCustomBoundsOnly -> false,
    customBounds -> Array.empty[String]
  )
}
