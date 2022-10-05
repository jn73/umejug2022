# Some fancy head line...

## Umetrics studio

**Moving our MVDA offerings to the cloud**

- Event driven micro-service architecture
- Kafka used as message broker
- Moving towards event sourcing
- Using Scala and Java when implementing backend services

## Why Scala?

- Functional language
    - Typed language that feels like a dynamic language
    - Static type checking
    - Large echo system
- JVM language
- Akka

## Why Akka

[Akka is a toolkit for building highly concurrent, distributed, and resilient message-driven applications for Java and Scala](http://akka.io)

- Akka actors
- Akka streaming
- Alpakka Kafka
- (Akka persistence)
- (Akka http)
- (Akka cluster)
- (Akka management)

### Changes to license

Starting with Akka 2.7 the license for all Akka modules will change from Apache 2.0 to the BSL v1.1

- Free for companies with less then $25m in annual revenue
- Reverts to the Apache 2.0 license after three years.
- [Pricing](https://www.lightbend.com/akka#pricing) per core. Standard and Enterprice alternatives available.

## A typical microservice

- Scala project that uses [SBT](https://www.scala-sbt.org/) as build tool.
- Application implemented as an Akka service.
- Using Akka streaming and Alpakka to consume messages from Kafka topics.
- Using [circe](https://circe.github.io/circe/) for simple and typesefe json serialization.
- Perform som application logic.
- Produce some resul (publishing some event on a kafka topic for example).
- Published as a docker image.
- Deployed as a kubernetes service.

## Akka actors

```scala
import akka.actor.typed.Behavior
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

object MyActor {

  sealed trait Protocol

  case class Ping(message: String) extends Protocol

  def apply(): Behavior[Protocol] = {
    Behaviors.receiveMessage { case Ping(message) =>
      println(s"Received a Ping: $message")
    }
  }

}

object MyApplication extends App {
  val actorSystem = ActorSystem[MyActor.Protocol](MyActor(), "system") // value with inferred type

  actorSystem ! MyActor.Ping("Hello Actor!")
}

```

## Akka streams

- Founding member of the [Reactive Streams initiative](https://www.reactive-streams.org/)
- Using constructs such as Source[T], Flow[A, B] and Sink[T] to process streaming data

```scala
import akka.stream.scaladsl.Source
import scala.util.Random
import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.RunnableGraph
import scala.concurrent.duration.DurationInt

val randomIntSource: Source[Int, NotUsed] = Source.fromIterator(() => Iterator.continually(new Random().nextInt()))

val multiplyBy10: Flow[Int, Int, NotUsed] = Flow.fromFunction(_ * 10)

val randomIntMultiplier: RunnableGraph[NotUsed] = randomIntSource
  .throttle(5, 1.second)
  .take(20000)
  .takeWhile(_ < 1000)
  .via(multiplyBy10)
  .to(Sink.foreach(v => println(s"Computed value: $v")))

randomIntMultiplier.run()
```

```scala
import akka.stream.scaladsl.Source
import scala.util.Random
import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.RunnableGraph
import scala.concurrent.duration.DurationInt

val src: Source[String, NotUsed] = Source.single("some value")
val firstFlow: Flow[String, String, NotUsed] = Flow.fromFunction(_.toUpperCase)
val secondFlow: Flow[String, String, NotUsed] = Flow.fromFunction(_.reverse)
val sink: Sink[String, NotUsed] = Sink.foreach(println)

// Attaching a Source to a Flow returns a new Source
val srcViaFlow: Source[Int, NotUsed] = src.via(firstFlow)

// Attaching a Flow to a Sink (to) returns a new Sink
val flowToSink: Sink[String, NotUsed] = secondFlow.to(sink)

srcViaFlow.runWith(flowToSink)

// ((src) ~> [firstFlow]) ~> <[secondFlow] ~> <sink>>

```

### Using alpakka to consume Kafka messages in a streaming way

```scala
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.ConsumerSettings
import akka.kafka.CommitterSettings
import akka.kafka.Subscriptions
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.RunnableGraph
import akka.kafka.ConsumerMessage
import scala.concurrent.Future
import akka.Done
import akka.NotUsed
import scala.concurrent.duration.DurationInt
import scala.util.Success

val consumerSettings: ConsumerSettings[String, String] = ???
val committerSettings: CommitterSettings = ???

val kafkaConsumerSource: Source[ConsumerMessage.CommittableMessage[String, String], Consumer.Control] = Consumer
  .committableSource(consumerSettings, Subscriptions.topics("someTopic"))

val committerSink: Sink[ConsumerMessage.Committable, Future[Done]] = Committer.sink(committerSettings)

val kafkaProcessingRunnableGraph: RunnableGraph[DrainingControl[Nothing]] = kafkaConsumerSource
  .throttle(10, 2.seconds)
  .via(
    Flow.fromFunction[ConsumerMessage.CommittableMessage[String, String], ConsumerMessage.Committable](f =>
      f.committableOffset
    )
  )
  .toMat(committerSink)(DrainingControl.apply)

val control: DrainingControl[Nothing] = kafkaProcessingRunnableGraph.run()

control.drainAndShutdown()

```

## Circe

```scala
import io.circe.parser.parse
import io.circe.ParsingFailure
import io.circe.Json
import io.circe.Decoder.Result
import io.circe.Error
import io.circe.generic.auto._

case class Person(name: String, age: Int)

val parsingResult: Either[ParsingFailure, Json] = parse("""{"name": "kalle", "age": 40}""")
val r: Json = parsingResult.getOrElse(Json.Null)

val personOrError: Either[Error, Person] = parsingResult.flatMap(_.as[Person])
val p: Person = personOrError.getOrElse(throw new Exception("Failed to parse json"))
```
