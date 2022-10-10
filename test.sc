import akka.stream.scaladsl._
import scala.util.Random
import akka.NotUsed
import akka.stream.scaladsl.Flow
import scala.concurrent.Future
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.ConfigFactory

val config = ConfigFactory.load()

implicit val actorSystem = ActorSystem(Behaviors.empty[NotUsed], "actorSystem", config)

val randomIntSource: Source[Int, NotUsed] = Source.fromIterator(() => Iterator.continually(new Random().nextInt()))


val multiplyBy10 : Flow[Int, Int, NotUsed] = Flow.fromFunction(_ * 10)


val a = Source.single(10).toMat(Sink.ignore)(Keep.both).run()