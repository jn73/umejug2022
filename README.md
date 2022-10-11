# Using Scala and Akka streams to consume and produce Kafka messages

[https://github.com/jn73/umejug2022](https://github.com/jn73/umejug2022)


### "Talk about Scala"

- How do you do that in an hour?
- Quick overview of Akka (actors/streaming)
- Look at some basic scala constructs (to understand the code)

## Umetrics studio backend basics

- Event driven microservice based architecture
- Kafka used as message broker
- Using Scala (2) and Java when implementing backend services

## Why Scala?

- Functional language
- JVM language
  - coexists with Java
  - large ecosystem
- Akka

## Why Akka?

[Akka is a toolkit for building highly concurrent, distributed, and resilient message-driven applications for Java and Scala](http://akka.io)

- Akka actors
- Akka streaming
- Alpakka Kafka
- (Akka persistence)
- (Akka http)
- (Akka cluster)

### Changes to license

Starting with Akka 2.7 the license for all Akka modules will change from Apache 2.0 to the BSL v1.1

- Free for companies with less than $25m in annual revenue
- Reverts to the Apache 2.0 license after three years.
- [Pricing](https://www.lightbend.com/akka#pricing) per core. Standard and Enterprice alternatives available.

## Getting started

- Install scala + sbt (https://sdkman.io)
  - sdk install scala 2.13.8
  - sdk install sbt 1.6.2
  - sbt new akka/akka-scala-seed.g8 (https://www.scala-sbt.org/1.x/docs/sbt-new-and-Templates.html)
- Use your favourite IDE (IntelliJ, ScalaIDE for Eclipse, VSCode)
- Online resources
  - https://docs.scala-lang.org
  - https://rockthejvm.com

## A "typical" microservice

- Scala project that uses [SBT](https://www.scala-sbt.org/) as build tool.
- Application implemented as an Akka service.
- Using Akka streaming and Alpakka to consume Kafka messages.
- Using [circe](https://circe.github.io/circe/) for json serialization.
- Perform som application logic.
- Produce some result (publishing some event on a kafka topic for example).
- Published as a docker image.
- Applications are deployed as a kubernetes service.

## Overview

- Go through some scala fundamentals to understand the examples
- Basic Akka actor
- Akka streaming example (Alpakka)
- Example application

## Some scala basics

### Values and variables

```scala
var mutableNumber = 10 // type is inferred
mutableNumber = 20

val immutableNumber: Int = 10

import scala.util.Random
lazy val someLazyValue = Random.nextInt()
```

[link: values_and_variables.sc](docs/values_and_variables.sc)

### Functions

```scala
def myFunction(name: String): String = {
  name.toUpperCase
}

val functionAsValue: String => Int = (inString) => inString.length
```

[link: functions.sc](docs/functions.sc)

### Classes and Objects

```scala

// regular class
class Person(val name: String, val age: Int, debug: Boolean) {
  if (debug) println(s"create person: $name")
}

val person = new Person("Kajsa", 30, false)

// case class
case class Shape(`type`: String)

val shape = Shape("square")

// object
object Shape {
  def newCircle = Shape("circle")

  def newSquare = Shape("square")
}

val circle = Shape.newCircle
```

[link: classes.sc](docs/classes.sc)

### Pattern matching

```scala
import scala.util._

val a: Try[Int] = Try(1 / 0)

a match {
  case Success(value) => println(s"success: $value")
  case Failure(error) => println(s"failure: $error")
}
```

[link: pattern_matching.sc](docs/pattern_matching.sc)

## Akka actors

- Single threaded message processor

```scala
import akka.actor.typed._
import akka.actor.typed.scaladsl._

object PingHandler {

  sealed trait Protocol

  case class Ping(message: String, replyTo: ActorRef[Pong]) extends Protocol

  case object Pong

  def apply(): Behavior[Protocol] = {
    Behaviors.receiveMessage { 
      case Ping(message, replyTo) =>
        println(s"Received a Ping: $message")
        replyTo ! Pong
    }
  }

}

object MyApplication extends App {

  val pingClient: ActorRef[PingHandler.Pong] = ???
  val pingHandler: ActorRef[PingHandler.Protocol] = ???

  pingHandler ! PingHandler.Ping("Hello Actor!", replyTo = pingClient)
}

```

## Akka streams

https://doc.akka.io/docs/akka/current/stream/index.html

- Using constructs such as Source[T], Flow[A, B] and Sink[T] to process streaming data

![](/Users/janne/projects/umejug2022/img/streams-1.png)

```scala
import akka.stream.scaladsl._
import akka.NotUsed

def multiplierFlow: Flow[Int, Int, NotUsed] = Flow.fromFunction(_ * 10)

val multiplierGraph: RunnableGraph[NotUsed] = Source
  .single(1)
  .via(multiplierFlow)
  .to(Sink.foreach(println)) // same as Sink.foreach(value => println(value))

multiplierGraph.run()
```

```scala
import akka.stream.scaladsl.Source
import scala.util.Random
import akka.NotUsed
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.RunnableGraph
import scala.concurrent.duration.DurationInt

val randomIntSource: Source[Int, NotUsed] = Source.fromIterator(() => Iterator.continually(new Random().nextInt()))

val randomIntMultiplier: RunnableGraph[NotUsed] = randomIntSource
  .throttle(5, 1.second)
  .take(200)
  .takeWhile(_ < 80)
  .map(_ * 10) // using map instead of Flow
  .to(Sink.foreach(v => println(s"Computed value: $v")))

randomIntMultiplier.run()
```

### Materialized views

- Previous examples only deals with payload
- How do we retrieve results from the stream that aren't just println etc?

```scala
import akka.stream.scaladsl._
import akka.NotUsed
import scala.concurrent._
import scala.concurrent.duration.DurationInt

val src: Source[Int, NotUsed] = Source(List(1, 2, 3))

// how do we read the actual first value?
val graph: RunnableGraph[NotUsed] = src
        .map(_ * 10)
        .to(Sink.head)

val graphWithMaterializedValue: RunnableGraph[Future[Int]] = src
        .map(_ * 10)
        .toMat(Sink.head)(Keep.right)

val materializedFuture: Future[Int] = graphWithMaterializedValue.run()

val headValue: Int = Await.result(materializedFuture, 1.second)
```

### Using alpakka to consume Kafka messages in a streaming way

```scala
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka._
import akka.stream.scaladsl._
import akka.kafka.ConsumerMessage
import scala.concurrent.Future
import akka.Done
import scala.concurrent.duration.DurationInt

val consumerSettings: ConsumerSettings[String, String] = ???
val committerSettings: CommitterSettings = ???

// a Source that reads kafka messages including the offset used to commit
val kafkaConsumerSource: Source[ConsumerMessage.CommittableMessage[String, String], Consumer.Control] = Consumer
  .committableSource(consumerSettings, Subscriptions.topics("someTopic"))

// a Sink that will use the incoming offset to commit the consumer position to kafka
val committerSink: Sink[ConsumerMessage.Committable, Future[Done]] = Committer.sink(committerSettings)

val kafkaProcessingRunnableGraph: RunnableGraph[DrainingControl[Nothing]] = kafkaConsumerSource
  .via(
    Flow.fromFunction[ConsumerMessage.CommittableMessage[String, String], ConsumerMessage.Committable](message =>
      // do some application logic..
      message.committableOffset
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
