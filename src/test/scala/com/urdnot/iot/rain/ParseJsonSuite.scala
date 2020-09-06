package com.urdnot.iot.rain

import com.typesafe.scalalogging.Logger
import com.urdnot.iot.rain.DataProcessing.parseRecord
import org.scalatest.flatspec.AsyncFlatSpec

class ParseJsonSuite extends AsyncFlatSpec with DataStructures {
  val log: Logger = Logger("rain")
  val validJson1: Array[Byte] = """{"timestamp": 1599367914208, "bucket_tip": 1}""".stripMargin.getBytes("utf-8")
  val validJson2: Array[Byte] = """{"timestamp": 1599367928864, "bucket_tip": 1}""".stripMargin.getBytes("utf-8")
  val validJson3: Array[Byte] = """{"timestamp": 1599367933734, "bucket_tip": 1}""".stripMargin.getBytes("utf-8")
  val validJsonReply1: RainBucketReading =  RainBucketReading(timestamp = Some(1599367914208L), bucket_tip = Some(1))
  val validJsonReply2: RainBucketReading =  RainBucketReading(timestamp = Some(1599367928864L), bucket_tip = Some(1))
  val validJsonReply3: RainBucketReading =  RainBucketReading(timestamp = Some(1599367933734L), bucket_tip = Some(1))
  val inValidJson: Array[Byte] = """{"timestamp: 1599367933734, "bucket_tip": 1}""".getBytes("utf-8")
  val errorReply = Left("""couldn't parse: {"timestamp: 1599367933734, "bucket_tip": 1} -- io.circe.ParsingFailure: expected : got 'bucket...' (line 1, column 30)""")

  behavior of "parsedJson"
  it should "Parse out the JSON data structure from the JSON string" in {
    val futureJson1 = parseRecord(validJson1, log)
    val futureJson2 = parseRecord(validJson2, log)
    val futureJson3 = parseRecord(validJson3, log)
    futureJson1 map { x =>
      assert(x == Right(validJsonReply1))
    }
    futureJson2 map { x =>
      assert(x == Right(validJsonReply2))
    }
    futureJson3 map { x =>
      assert(x == Right(validJsonReply3))
    }
  }
  it should "Return an error when there is a bad JSON string" in {
    val futureJson = parseRecord(inValidJson, log)
    futureJson map { x =>
      assert(x == errorReply)
    }
  }
}

