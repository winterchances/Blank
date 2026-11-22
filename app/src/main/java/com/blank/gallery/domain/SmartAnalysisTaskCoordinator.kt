package com.blank.gallery.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SmartAnalysisType {
    DUPLICATES,
    SIMILAR_PHOTOS,
    BURST_PHOTOS,
    SCREENSHOTS,
    LARGE_MEDIA
}

sealed interface SmartAnalysisTaskState {
    data object Idle : SmartAnalysisTaskState

    data class Running(
        val parameterKey: String,
        val completed: Int,
        val total: Int
    ) : SmartAnalysisTaskState

    data class Cancelling(
        val parameterKey: String
    ) : SmartAnalysisTaskState

    data class Completed(
        val parameterKey: String,
        val completed: Int,
        val total: Int,
        val completedAtMillis: Long
    ) : SmartAnalysisTaskState

    data class Failed(
        val parameterKey: String,
        val message: String
    ) : SmartAnalysisTaskState

    data class Cancelled(
        val parameterKey: String
    ) : SmartAnalysisTaskState
}

/**
 * Coordinates one in-flight task per analysis type.
 *
 * Repeated requests with identical parameters share the same result. A request
 * with different parameters is rejected while that analysis type is running,
 * so callers cannot accidentally receive a result for another configuration.
 *
 * Runtime task state is intentionally kept in memory: jobs cannot be resumed
 * after process death. Per-asset hashes remain persisted by MediaAnalysisCache.
 */
class SmartAnalysisTaskCoordinator {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private val activeTasks = mutableMapOf<SmartAnalysisType, ActiveTask>()

    private val _states = MutableStateFlow<Map<SmartAnalysisType, SmartAnalysisTaskState>>(
        emptyMap()
    )
    val states: StateFlow<Map<SmartAnalysisType, SmartAnalysisTaskState>> =
        _states.asStateFlow()

    suspend fun <T : Any> execute(
        type: SmartAnalysisType,
        parameterKey: String,
        analysis: suspend (onProgress: suspend (completed: Int, total: Int) -> Unit) -> T
    ): T {
        val task = mutex.withLock {
            val existing = activeTasks[type]
            if (existing != null) {
                require(existing.parameterKey == parameterKey) {
                    "This analysis is already running with different settings."
                }
                existing
            } else {
                val result = CompletableDeferred<Any>()
                val job = scope.launch(start = CoroutineStart.LAZY) {
                    var completed = 0
                    var total = 0
                    try {
                        val value = analysis { nextCompleted, nextTotal ->
                            completed = nextCompleted.coerceAtLeast(0)
                            total = nextTotal.coerceAtLeast(0)
                            setState(
                                type,
                                SmartAnalysisTaskState.Running(
                                    parameterKey = parameterKey,
                                    completed = completed,
                                    total = total
                                )
                            )
                        }
                        setState(
                            type,
                            SmartAnalysisTaskState.Completed(
                                parameterKey = parameterKey,
                                completed = completed,
                                total = total,
                                completedAtMillis = System.currentTimeMillis()
                            )
                        )
                        result.complete(value)
                    } catch (cancelled: CancellationException) {
                        setState(type, SmartAnalysisTaskState.Cancelled(parameterKey))
                        result.cancel(cancelled)
                        throw cancelled
                    } catch (error: Exception) {
                        setState(
                            type,
                            SmartAnalysisTaskState.Failed(
                                parameterKey = parameterKey,
                                message = error.message ?: error::class.java.simpleName
                            )
                        )
                        result.completeExceptionally(error)
                    } finally {
                        mutex.withLock {
                            if (activeTasks[type]?.result === result) {
                                activeTasks.remove(type)
                            }
                        }
                    }
                }

                val created = ActiveTask(parameterKey, result, job)
                activeTasks[type] = created
                setState(
                    type,
                    SmartAnalysisTaskState.Running(
                        parameterKey = parameterKey,
                        completed = 0,
                        total = 0
                    )
                )
                job.start()
                created
            }
        }

        @Suppress("UNCHECKED_CAST")
        return task.result.await() as T
    }

    suspend fun cancel(type: SmartAnalysisType) {
        mutex.withLock {
            activeTasks[type]?.let { task ->
                setState(type, SmartAnalysisTaskState.Cancelling(task.parameterKey))
                task.job.cancel(
                    CancellationException("Smart analysis cancelled by the user.")
                )
            }
        }
    }

    private fun setState(type: SmartAnalysisType, state: SmartAnalysisTaskState) {
        _states.update { current -> current + (type to state) }
    }

    private data class ActiveTask(
        val parameterKey: String,
        val result: CompletableDeferred<Any>,
        val job: Job
    )
}
