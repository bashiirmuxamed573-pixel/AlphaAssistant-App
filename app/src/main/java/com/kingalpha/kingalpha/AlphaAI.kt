package com.kingalpha.kingalpha

enum class AlphaCommand {
    LOCK_DEVICE,
    UNKNOWN
}

object AlphaAI {

    fun processCommand(input: String): AlphaCommand {
        val command = input
            .trim()
            .lowercase()
            .replace(",", " ")
            .replace(".", " ")
            .replace("  ", " ")

        return when {
            command.contains("lock is geli") ||
            command.contains("telefoonka xiro") ||
            command.contains("telefoonka xir") ||
            command.contains("telefoonka quful") ||
            command.contains("quful telefoonka") ||
            command.contains("lock telefoonka") ||
            command == "quful" -> {
                AlphaCommand.LOCK_DEVICE
            }

            else -> AlphaCommand.UNKNOWN
        }
    }
}