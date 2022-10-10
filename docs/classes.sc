class Person(name: String, age: Int, val student: Boolean) {

  println("This is the default constructor")

  def this(name: String) = this(name, 50, false)

  def nameToUpper = name.toUpperCase

}

val kalle = new Person("kalle", 20, false)

kalle.nameToUpper
kalle.student
// kalle.name