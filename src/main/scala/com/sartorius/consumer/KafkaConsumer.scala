package com.sartorius.consumer

import akka.Done
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{ConsumerMessage, Subscriptions}
import akka.util.Timeout
import com.sartorius.Settings
import com.sartorius.Settings.{committerSettings, consumerSettings}
import com.sartorius.consumer.Protocol.Measurement
import io.circe
import io.circe.generic.auto._
import io.circe.parser.parse
import org.apache.kafka.clients.consumer.ConsumerRecord

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object KafkaConsumer {

  def apply(
    messageProcessor: ActorRef[MessageProcessor.Protocol]
  )(implicit system: ActorSystem[Nothing]): DrainingControl[Done] = {

    implicit val askTimeout: Timeout = Timeout(2.seconds)

    def parseMeasurement(message: String): Either[circe.Error, Measurement] = parse(message).flatMap(_.as[Measurement])

    Consumer
      .committableSource(consumerSettings, Subscriptions.topics(Settings.topicName))
      .throttle(2, 1.seconds)
      .map {
        case CommittableMessage(
              record: ConsumerRecord[String, String],
              committableOffset: ConsumerMessage.CommittableOffset
            ) =>
          (committableOffset, parseMeasurement(record.value()))
      }
      .mapAsync(1) {
        case (offset, Right(measurement)) =>
          messageProcessor
            .ask(MessageProcessor.AddMeasurement(measurement, _))
            .map(_ => offset)
        case (offset, Left(circeError)) =>
          println(s"Failed to decode json: $circeError")
          Future.successful(offset)
      }
      .toMat(Committer.sink(committerSettings))(DrainingControl.apply)
      .run()

  }

}
