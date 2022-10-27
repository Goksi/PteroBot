package tech.goksi.pterobot.util

private const val EMPTY_CHAR = '□'
private const val FULL_CHAR = '■'

data class MemoryBar(val usedMemory: Long, val maxMemory: Long) {

    override fun toString(): String {
        val builder = StringBuilder()
        val oneBlock = (maxMemory / 10).toInt()
        val numberOfFull = (usedMemory / oneBlock).toInt()

        for (i in 1..10) {
            if (i <= numberOfFull) {
                builder.append(FULL_CHAR)
            } else {
                builder.append(EMPTY_CHAR)
            }
        }
        return builder.toString()
    }
}
