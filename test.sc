import akka.stream.scaladsl.Source
import scala.util.Random
import akka.NotUsed
import akka.stream.scaladsl.Flow

val randomIntSource: Source[Int, NotUsed] = Source.fromIterator(() => Iterator.continually(new Random().nextInt()))


val multiplyBy10 : Flow[Int, Int, NotUsed] = Flow.fromFunction(_ * 10)

