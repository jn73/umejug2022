// Pattern matching

val a = "kalle anka"

// using _ to "catch all"
a match {
  case "kalle anka" => true
  case _ => false
}

// using guards when matching
a match {
  case s if s.startsWith("kalle") => s.toUpperCase
  case s => s.reverse
}

// matching function arguments
val multiply : Int => Int = {
  case i if i % 2 == 0 => i * 10
  case i => i * 100
}

multiply(20)

// matching regex
val expr = """kalle (\S+)""".r

a match {
  case expr(surname) => s"Hello mr. $surname"
}

// mattching case classes
case class Person(name: String, age: Int)

val kalle = Person("Kalle Anka", 25)

kalle match {
  case Person(name, 25) if name startsWith "Kalle" => name
}


