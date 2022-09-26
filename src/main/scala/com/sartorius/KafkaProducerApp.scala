package com.sartorius

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.kafka.scaladsl.Producer
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Source}
import com.sartorius.Protocol.Measurement
import org.apache.kafka.clients.producer.ProducerRecord
import io.circe.syntax._
import io.circe.generic.auto._

import scala.util.Random

object KafkaProducerApp extends App {

  implicit val system: ActorSystem[NotUsed] = ActorSystem(Behaviors.empty, "producerSystem")

  val producer = Source
    .queue[Protocol.Measurement](1000, OverflowStrategy.backpressure, 1)
    .map(cmd => new ProducerRecord[String, String](Settings.topicName, cmd.asJson.toString()))
    .toMat(Producer.plainSink(Settings.producerSettings(system)))(Keep.left)
    .run()

  1 to 10 foreach (_ => producer.offer {
    println("Offer to kafka")
    Measurement(Random.nextDouble())
  })

}
