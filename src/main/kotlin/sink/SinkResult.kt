package leia.sink

sealed class SinkResult {
    object SuccessfullyWritten : SinkResult()
    data class WritingFailed(val exc: Exception): SinkResult()
}