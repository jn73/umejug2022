package com.sartorius

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.kafka.{CommitterSettings, ConsumerSettings, ProducerSettings}
import com.typesafe.config.ConfigFactory
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

object Settings {

  private val config = ConfigFactory.load()

  val consumerSettings: ConsumerSettings[String, String] = ConsumerSettings(
    config.getConfig("akka.kafka.consumer"),
    new StringDeserializer,
    new StringDeserializer
  )
    .withBootstrapServers("localhost:9092")
    .withGroupId("umeJug")

  val committerSettings: CommitterSettings = CommitterSettings(config.getConfig(CommitterSettings.configPath))

  def producerSettings(system: ActorSystem[NotUsed]): ProducerSettings[String, String] = {
    ProducerSettings(system, new StringSerializer, new StringSerializer)
      .withBootstrapServers("localhost:9092")

  }

  val topicName = "measurementTopic"
}
