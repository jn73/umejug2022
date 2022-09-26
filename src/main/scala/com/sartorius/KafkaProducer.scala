package com.sartorius

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.kafka.scaladsl.Producer
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Source, SourceQueue}
import org.apache.kafka.clients.producer.ProducerRecord
import io.circe.syntax._
import io.circe.generic.auto._

object KafkaProducer {

  def apply(implicit system: ActorSystem[NotUsed]): SourceQueue[Protocol.Measurement] = {
    Source
      .queue[Protocol.Measurement](1000, OverflowStrategy.backpressure, 1)
      .map(cmd => new ProducerRecord[String, String](Settings.topicName, cmd.asJson.toString()))
      .toMat(Producer.plainSink(Settings.producerSettings(system)))(Keep.left)
      .run()
  }

}
