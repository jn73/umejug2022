package com.sartorius.producer

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.kafka.scaladsl.Producer
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Source}
import com.sartorius.Settings
import com.sartorius.consumer.Protocol.Measurement
import io.circe.generic.auto._
import io.circe.syntax._
import org.apache.kafka.clients.producer.ProducerRecord

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

object KafkaProducerApp extends App {

  implicit val system: ActorSystem[NotUsed] = ActorSystem(Behaviors.empty, "producerSystem")

  val producer = Source
    .queue[Measurement](1000, OverflowStrategy.backpressure, 1)
    .map(message => new ProducerRecord[String, String](Settings.topicName, message.asJson.toString()))
    .toMat(Producer.plainSink(Settings.producerSettings(system)))(Keep.left)
    .run()

  1 to 10 foreach (_ =>
    producer.offer {
      val measurement = Random.nextDouble()
      println(s"Offer measurement to kafka producer: $measurement")
      Measurement(measurement, Instant.now())
    }
  )

  producer.watchCompletion().onComplete { _ =>
    // terminate actor system regardless of outcome
    system.terminate()
  }

  producer.complete()

}
