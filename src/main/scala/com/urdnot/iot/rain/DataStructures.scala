package com.urdnot.iot.rain

trait DataStructures {
  final case class RainBucketReading(
                                      timestamp: Option[Long],
                                      bucket_tip: Option[Int]
                                    )
}
