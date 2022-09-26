package com.sartorius

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.sartorius.Protocol.Measurement

object MessageProcessor {

  sealed trait Protocol

  case class AddMeasurement(measurement: Measurement, replyTo: ActorRef[Done]) extends Protocol

  case class GetSum(replyTo: ActorRef[Double]) extends Protocol

  def apply(measurements: Seq[Measurement] = List.empty): Behavior[Protocol] = Behaviors.receiveMessage {
    case AddMeasurement(measurement, replyTo) =>
      println("added measurement: " + measurement.value)
      replyTo ! Done
      MessageProcessor(measurements :+ measurement)

    case GetSum(replyTo) =>
      replyTo ! measurements.map(_.value).sum
      Behaviors.same
  }

}
