package com.sartorius.producer

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.kafka.scaladsl.Producer
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Source}
import akka.{Done, NotUsed}
import com.sartorius.Settings
import com.sartorius.consumer.Protocol.Measurement
import io.circe.generic.auto._
import io.circe.syntax._
import org.apache.kafka.clients.producer.ProducerRecord

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Random, Success}

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
      Measurement(measurement)
    }
  )

  producer.watchCompletion().onComplete {
    case Success(Done) =>
      println("Producer stream terminated")
      system.terminate()
    case Failure(exception) =>
      println(s"Failed to terminate producer stream: ${exception.getMessage}")
      system.terminate()
  }

  producer.complete()

}
