fun add(a: Int, b: Int): Int {
    return a + b
}

fun multiply(a: Int, b: Int): Int {
    return a * b
}

fun divide(a: Int, b: Int): Double {
    if (b == 0) {
        throw IllegalArgumentException("Деление на ноль недопустимо")
    }
    return a.toDouble() / b
}
