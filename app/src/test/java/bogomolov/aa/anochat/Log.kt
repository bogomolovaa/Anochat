@file:JvmName("Log")

package android.util

fun i(tag: String, msg: String): Int {
    println("I: $tag: $msg")
    return 0
}

fun d(tag: String, msg: String): Int {
    println("D: $tag: $msg")
    return 0
}

fun w(tag: String, msg: String): Int {
    println("W: $tag: $msg")
    return 0
}
