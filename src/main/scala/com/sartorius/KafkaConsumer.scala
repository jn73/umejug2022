package com.sartorius

import akka.Done
import akka.actor.typed.scaladsl.AskPattern.{Askable, schedulerFromActorSystem}
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.Subscriptions
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.util.Timeout
import com.sartorius.Protocol.Measurement
import com.sartorius.Settings.{committerSettings, consumerSettings}
import io.circe
import io.circe.generic.auto._
import io.circe.parser.parse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object KafkaConsumer {

  def apply(messageProcessor: ActorRef[MessageProcessor.Protocol])(implicit system: ActorSystem[Nothing]): DrainingControl[Done] = {

    implicit val askTimeout: Timeout = Timeout(2.seconds)

    def parseMeasurement(message: String): Either[circe.Error, Measurement] = parse(message).flatMap(_.as[Measurement])

    Consumer
      .committableSource(consumerSettings, Subscriptions.topics(Settings.topicName))
      .map {
        case CommittableMessage(record, committableOffset) => (committableOffset, parseMeasurement(record.value()))
      }
      .throttle(2, 1.seconds)
      .mapAsync(2) {
        case (offset, Right(measurement)) => messageProcessor
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
