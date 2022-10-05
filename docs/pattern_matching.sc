// Pattern matching

val a = "kalle anka"

a match {
  case "kalle anka" => true
  case _ => false
}

a match {
  case s => s.toUpperCase
}

a match {
  case s if s.startsWith("kalle") => s.toUpperCase
  case s => s.reverse
}

val expr = """kalle (\S+)""".r

a match {
  case expr(surname) => s"Hello mr. $surname"
}

case class Person(name: String, age: Int)

val kalle = Person("Kalle Anka", 25)

kalle match {
  case Person(name, 25) if name startsWith "Kalle" => name
}
