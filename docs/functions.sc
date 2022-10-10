def multiplyBy10(a: Int) : Int = {
  a * 10
}

// return type inferred
// no brackets needed on single expression functions
def multiplyBy10_short(a: Int) = a * 10

// function definition argument type inferred
val multiplyBy10_asValue : Int => Int = a => a * 10

// operating on argument directly with _
val multiplyBy10_asValue2 : Int => Int = _ * 10

// type of val is inferred from the function definition
val multiplyBy10_inferredValue = (a: Int) => a * 10


// partially applied function
def multiplyNumbers(a: Int, b: Int) = a * b

val multiplyBy10Partial: Int => Int = multiplyNumbers(10, _)
multiplyBy10Partial(200)