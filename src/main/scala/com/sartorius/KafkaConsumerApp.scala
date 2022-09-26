package com.sartorius

import akka.actor.CoordinatedShutdown
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import akka.kafka.scaladsl.Consumer
import akka.{Done, NotUsed}

import scala.concurrent.ExecutionContext.Implicits.global

object KafkaConsumerApp extends App {

  object RootBehavior {

    def apply(): Behavior[NotUsed] = Behaviors.setup { ctx =>

      val messageProcessor =
        ctx.spawn(MessageProcessor(), "messageProcessorActor")

      val kafkaConsumerControl: Consumer.DrainingControl[Done] =
        KafkaConsumer(messageProcessor)(ctx.system)

      CoordinatedShutdown(ctx.system).addTask(CoordinatedShutdown.PhaseServiceUnbind, "shutDownKafkaConsumer")(() =>
        kafkaConsumerControl.drainAndShutdown()
      )

      Behaviors.empty
    }

  }

  val system = ActorSystem(RootBehavior(), "consumerSystem")
}
