package com.sartorius.consumer

import akka.actor.CoordinatedShutdown
import akka.{Done, NotUsed}
import akka.actor.typed.{Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import akka.kafka.scaladsl.Consumer

import scala.concurrent.ExecutionContext.Implicits.global

object KafkaConsumerRootBehavior {

  def apply(): Behavior[NotUsed] = Behaviors.setup { ctx =>
    val messageProcessor =
      ctx.spawn(MessageProcessor(), "messageProcessorActor")

    ctx.watch(messageProcessor)

    // create a KafkaConsumer Stream. Also passing along the messageProcessor ActorRef
    val kafkaConsumer: Consumer.DrainingControl[Done] =
      KafkaConsumer(messageProcessor)(ctx.system)

    CoordinatedShutdown(ctx.system)
      .addTask(CoordinatedShutdown.PhaseServiceUnbind, "shutDownKafkaConsumer")(() => kafkaConsumer.drainAndShutdown())

    Behaviors.receiveSignal { case (ctx, Terminated(actorRef)) =>
      ctx.log.error(s"Actor terminated: ${actorRef.path}")
      Behaviors.stopped
    }
  }

}
