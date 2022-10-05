package com.sartorius.consumer

object Protocol {

  case class Pair[A, B](first: A, second: B)

  case class Measurement(value: Double)

}
