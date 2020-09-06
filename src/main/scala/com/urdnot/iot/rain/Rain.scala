package com.urdnot.iot.rain

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.typesafe.config.Config
import com.typesafe.scalalogging.{LazyLogging, Logger}
import com.urdnot.iot.rain.DataProcessing.parseRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object Rain extends LazyLogging with DataStructures {

  implicit val system: ActorSystem = ActorSystem("rain_processor")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = materializer.executionContext
  val log: Logger = Logger("rain")

  val consumerConfig: Config = system.settings.config.getConfig("akka.kafka.consumer")
  val envConfig: Config = system.settings.config.getConfig("env")
  val bootstrapServers: String = consumerConfig.getString("kafka-clients.bootstrap.servers")
  val consumerSettings: ConsumerSettings[String, Array[Byte]] =
    ConsumerSettings(consumerConfig, new StringDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers(bootstrapServers)

  val INFLUX_URL: String = "http://" + envConfig.getString("influx.host") + ":" + envConfig.getInt("influx.port") + envConfig.getString("influx.route")
  val INFLUX_USERNAME: String = envConfig.getString("influx.username")
  val INFLUX_PASSWORD: String = envConfig.getString("influx.password")
  val INFLUX_DB: String = envConfig.getString("influx.database")
  val INFLUX_MEASUREMENT: String = "rainBucket"
  val SOURCE_HOST: String = "pi-weather"
  val SENSOR: String = "rainBucket"


  Consumer
    .plainSource(consumerSettings, Subscriptions.topics(envConfig.getString("kafka.topic")))
    .map { consumerRecord =>
      parseRecord(consumerRecord.value(), log)
        .onComplete {
          case Success(x) => x match {
            case Right(valid) =>
              val data = {
                s"""$INFLUX_MEASUREMENT,host=$SOURCE_HOST,sensor=$SENSOR bucketTip=${valid.bucket_tip.get} ${valid.timestamp.get}000000""".stripMargin
              }
              val runInflux: Future[HttpResponse] = Http().singleRequest(HttpRequest(
                method = HttpMethods.POST,
                uri = Uri(INFLUX_URL).withQuery(
                  Query(
                    "bucket" -> INFLUX_DB,
                    "precision" -> "ns"
                  )
                ),
                headers = Seq(
                  Authorization(BasicHttpCredentials(INFLUX_USERNAME, INFLUX_PASSWORD))),
                entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, data)
              ))
              runInflux.onComplete{{
                case Success(res) => res match {
                  case res if res.status.isFailure() => log.error(res.status.reason() + ": " + res.status.defaultMessage() + " Influx Message: " + res.headers(2))
                  case res if res.status.isSuccess() => log.debug(res.toString())
                  case _ => log.error("Influx failure: " + res)
                }
                case Failure(e) => log.error("Unable to connect to Influx: " + e.getMessage)
              }}
              valid
            case Left(invalid) => log.error(invalid)
          }
          case Failure(exception) => log.error(exception.getMessage)
        }
    }
    .toMat(Sink.ignore)(DrainingControl.apply)
    .run()
}
