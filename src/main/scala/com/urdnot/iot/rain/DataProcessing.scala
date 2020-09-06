package com.urdnot.iot.rain

import com.typesafe.scalalogging.Logger
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.jawn.decode

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DataProcessing extends DataStructures {

  def parseRecord(record: Array[Byte], log: Logger): Future[Either[String, RainBucketReading]] = Future {
    implicit val decoder: Decoder[RainBucketReading] = deriveDecoder[RainBucketReading]
//        println(record.map(_.toChar).mkString)
    decode[RainBucketReading](record.map(_.toChar).mkString) match {
      case Right(x) => Right(x)
      case Left(x) => val errorMessage = "couldn't parse: " + record.map(_.toChar).mkString + " -- " + x
        log.error(errorMessage)
        Left(errorMessage)
    }
  }
}