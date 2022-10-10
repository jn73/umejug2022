package com.sartorius.consumer

import java.time.Instant

object Protocol {

  case class Measurement(value: Double, time: Instant)

}
