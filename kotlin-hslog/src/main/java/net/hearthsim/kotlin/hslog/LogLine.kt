package net.hearthsim.kotlin.hslog

import java.util.regex.Pattern

class LogLine(
        val level: String,
        val line: String,
        val method: String?,
        val seconds: Int
) {
    companion object {
        private fun getSeconds(time: String): Int {
            val a = time.split(":")
            if (a.size < 3) {
                return 0
            }

            var sec = 0
            sec += Integer.parseInt(a[0]) * 3600
            sec += Integer.parseInt(a[1]) * 60
            sec += java.lang.Float.parseFloat(a[2]).toInt()

            return sec
        }

        val PATTERN_WITH_METHOD = Regex("([^ ]) +([^ ]*) +([^ ]*) +(.*)")
        val PATTERN =  Regex("([^ ]) +([^ ]*) +(.*)")

        fun parseLineWithMethod(line: String, logger: ((String, Array<out String>) -> Unit)?): LogLine? {

            //D 19:48:03.8108410 GameState.DebugPrintPower() -     Player EntityID=3 PlayerID=2 GameAccountId=redacted

            val matcher = PATTERN_WITH_METHOD.matchEntire(line)

            if (matcher == null) {
                logger?.invoke("invalid line: $line", emptyArray())
                return null
            }

            val level = matcher.groupValues[1]
            val seconds: Int
            try {
                seconds = getSeconds(matcher.groupValues[2])
            } catch (e: NumberFormatException) {
                logger?.invoke("bad time: $line", emptyArray())
                return null
            }

            val method = matcher.groupValues[3]

            val remaining = matcher.groupValues[4]
            if (!remaining.startsWith("- ")) {
                logger?.invoke("missing '-': $line", emptyArray())
                return null
            }

            return LogLine(
                    level = level,
                    line = remaining.substring(2),
                    method = method,
                    seconds = seconds
            )
        }

        fun parseLine(line: String): LogLine? {
            //I 21:35:38.5792300 # Deck ID: 1384195626

            val matcher = PATTERN.matchEntire(line)

            if (matcher == null) {
                return null
            }

            val level = matcher.groupValues[1]
            val seconds: Int
            try {
                seconds = getSeconds(matcher.groupValues[2])
            } catch (e: NumberFormatException) {
                return null
            }

            return LogLine(
                    level = level,
                    line = matcher.groupValues[3],
                    seconds = seconds,
                    method = null
            )
        }
    }
}