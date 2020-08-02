package ru.skillbranch.kotlinexample.extensions
fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> =
    run {
        toMutableList().apply {
            if (size > 0) {
                listIterator(size).apply {
                    do {
                        val element=previous()
                        remove()
                    } while (hasPrevious() && !predicate(element))
                }
            }
        }.toList()
    }