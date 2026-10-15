package com.example.platform.obd

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

class ObdCommandQueue(
    private val rawExecutor: suspend (String) -> String
) {
    private val mutex = Mutex()

    suspend fun execute(command: ObdCommand): ObdResponse {
        return mutex.withLock {
            val result = withTimeoutOrNull(command.timeoutMs) {
                try {
                    val raw = rawExecutor(command.command)
                    val cleaned = Elm327Parser.cleanResponse(raw)
                    val errorToken = Elm327Parser.checkError(cleaned)
                    if (errorToken != null) {
                        ObdResponse(
                            raw = raw,
                            isSuccess = false,
                            errorMessage = "Adapter returned error: $errorToken"
                        )
                    } else {
                        ObdResponse(
                            raw = raw,
                            isSuccess = true
                        )
                    }
                } catch (e: Exception) {
                    ObdResponse(
                        raw = "",
                        isSuccess = false,
                        errorMessage = e.message ?: "Execution failed"
                    )
                }
            }
            result ?: ObdResponse(
                raw = "",
                isSuccess = false,
                errorMessage = "Command timeout after ${command.timeoutMs}ms"
            )
        }
    }
}
