package com.sartorius.consumer

import akka.actor.typed.ActorSystem

import scala.concurrent.ExecutionContext.Implicits.global

object KafkaConsumerApp extends App {

  val system = ActorSystem(KafkaConsumerRootBehavior(), "consumerSystem")
  system.whenTerminated.foreach(_ => println("KafkaConsumerApp shut down"))

}
