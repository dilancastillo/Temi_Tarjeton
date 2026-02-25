package com.example.temitarjeton.data.temi

import com.robotemi.sdk.Robot
import com.robotemi.sdk.TtsRequest
import com.robotemi.sdk.constants.HardButton
import com.robotemi.sdk.listeners.OnButtonStatusChangedListener
import com.robotemi.sdk.listeners.OnGoToLocationStatusChangedListener
import com.robotemi.sdk.listeners.OnUserInteractionChangedListener
import com.robotemi.sdk.permission.OnRequestPermissionResultListener
import com.robotemi.sdk.permission.Permission
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withTimeout
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Controller “del robot” (Data layer).
 *
 * Responsabilidades:
 * 1) TTS (speak + speakAndWait)
 * 2) Detección de interacción física (touch / botones) -> [interactionEvents]
 * 3) Patrulla “manual” por ubicaciones (goTo en bucle), con retry 1 vez y luego skip.
 */
class TemiRobotController :
    Robot.TtsListener,
    OnUserInteractionChangedListener,
    OnButtonStatusChangedListener,
    OnGoToLocationStatusChangedListener {

    private val robot: Robot = Robot.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // -------- TTS --------

    private val pendingTts = ConcurrentHashMap<UUID, CompletableDeferred<Unit>>()

    // -------- Interacciones físicas (robot / hard buttons) --------

    private val _interactionEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val interactionEvents: SharedFlow<Unit> = _interactionEvents.asSharedFlow()

    // -------- Patrulla --------

    private var patrolJob: Job? = null
    private var goToGate: CompletableDeferred<GoToOutcome>? = null
    private var goToLocation: String? = null
    private var mapPermissionGate: CompletableDeferred<Boolean>? = null

    private sealed interface GoToOutcome {
        data object Complete : GoToOutcome
        data object Abort : GoToOutcome
    }

    init {
        robot.addTtsListener(this)
        robot.addOnUserInteractionChangedListener(this)
        robot.addOnButtonStatusChangedListener(this)
        robot.addOnGoToLocationStatusChangedListener(this)
        robot.addOnRequestPermissionResultListener(this)
    }

    // ---------------------------
    // TTS
    // ---------------------------

    fun speak(text: String) {
        robot.speak(TtsRequest.create(text))
    }

    /** Habla y espera a que termine (COMPLETED / ERROR / CANCELED / NOT_ALLOWED). */
    suspend fun speakAndWait(text: String, timeoutMs: Long = 20_000) {
        val req = TtsRequest.create(text)
        val gate = CompletableDeferred<Unit>()
        pendingTts[req.id] = gate

        robot.speak(req)

        try {
            withTimeout(timeoutMs) { gate.await() }
        } finally {
            pendingTts.remove(req.id)
        }
    }

    override fun onTtsStatusChanged(ttsRequest: TtsRequest) {
        when (ttsRequest.status) {
            TtsRequest.Status.COMPLETED,
            TtsRequest.Status.ERROR,
            TtsRequest.Status.NOT_ALLOWED,
            TtsRequest.Status.CANCELED -> pendingTts.remove(ttsRequest.id)?.complete(Unit)

            else -> Unit // PENDING / PROCESSING / STARTED
        }
    }

    // ---------------------------
    // Interacción física
    // ---------------------------

    /**
     * Temi reporta “interacción” cuando hay touch / conversación / detección / etc.
     * Para nuestro caso: si es true, lo tratamos como “alguien tocó el robot”.
     */
    override fun onUserInteraction(isInteracting: Boolean) {
        if (isInteracting) _interactionEvents.tryEmit(Unit)
    }

    /**
     * Reporta click de botones físicos (“hard buttons”), incluyendo el botón de interacción/seguimiento.
     */
    override fun onButtonStatusChanged(hardButton: HardButton, status: HardButton.Status) {
        if (status == HardButton.Status.CLICKED) {
            _interactionEvents.tryEmit(Unit)
        }
    }

    // ---------------------------
    // Patrulla
    // ---------------------------

    fun stopMovement() {
        robot.stopMovement()
    }

    /**
     * Devuelve las ubicaciones guardadas que coinciden con:
     * ubicacion1, ubicacion2, ubicacion3... (ordenadas por el número).
     */
    fun buildPatrolRoute(
        prefix: String = "ubicacion",
        maxLocations: Int = 3,
    ): List<String> {
        val saved = robot.locations

        fun parseIndex(name: String): Int? {
            val lower = name.trim().lowercase()
            val p = prefix.lowercase()
            if (!lower.startsWith(p)) return null
            val suffix = lower.removePrefix(p).trim()
            return suffix.toIntOrNull()
        }

        return saved
            .mapNotNull { name ->
                val idx = parseIndex(name) ?: return@mapNotNull null
                idx to name
            }
            .sortedBy { it.first }
            .take(maxLocations)
            .map { it.second }
    }

    /**
     * Inicia una patrulla manual: goTo -> esperar COMPLETE/ABORT -> siguiente.
     * Si ABORT, reintenta 1 vez y luego salta.
     */
    fun startPatrol(route: List<String>) {
        stopPatrol()
        if (route.isEmpty()) return

        patrolJob = scope.launch {
            var index = 0
            while (isActive) {
                val location = route[index]
                val succeeded = goToWithRetry(location = location, retries = 1)

                // Si falla y no hay éxito, simplemente saltamos al siguiente.
                index = (index + 1) % route.size

                // micro-delay para evitar spam de IPC si algo se queda reportando estados muy rápido
                if (!succeeded) delay(150)
            }
        }
    }

    fun stopPatrol() {
        patrolJob?.cancel()
        patrolJob = null
        goToGate?.cancel()
        goToGate = null
        goToLocation = null
        stopMovement()
    }

    private suspend fun goToWithRetry(location: String, retries: Int): Boolean {
        repeat(retries + 1) { attempt ->
            val outcome = goToAndAwait(location)
            if (outcome == GoToOutcome.Complete) return true
            if (attempt < retries) {
                // Reintento rápido
                delay(250)
            }
        }
        return false
    }

    private suspend fun goToAndAwait(location: String): GoToOutcome {
        // Configuramos “gate” para esperar COMPLETE/ABORT.
        val gate = CompletableDeferred<GoToOutcome>()
        goToGate = gate
        goToLocation = location

        // noRotationAtEnd = true => más “nonstop” (evita giro al final)
        robot.goTo(location = location, noRotationAtEnd = true)

        return try {
            // 2 minutos por destino, ajustable.
            withTimeout(120_000) { gate.await() }
        } catch (_: Exception) {
            GoToOutcome.Abort
        } finally {
            if (goToGate == gate) {
                goToGate = null
                goToLocation = null
            }
        }
    }

    override fun onGoToLocationStatusChanged(
        location: String,
        status: String,
        descriptionId: Int,
        description: String,
    ) {
        val target = goToLocation ?: return
        if (!location.equals(target, ignoreCase = true)) return

        when (status) {
            OnGoToLocationStatusChangedListener.COMPLETE -> {
                goToGate?.complete(GoToOutcome.Complete)
            }

            OnGoToLocationStatusChangedListener.ABORT -> {
                goToGate?.complete(GoToOutcome.Abort)
            }
        }
    }

    fun dispose() {
        stopPatrol()
        robot.removeTtsListener(this)
        robot.removeOnUserInteractionChangedListener(this)
        robot.removeOnButtonStatusChangedListener(this)
        robot.removeOnGoToLocationStatusChangedListener(this)
        robot.removeOnRequestPermissionResultListener(this)
        mapPermissionGate?.cancel()
        mapPermissionGate = null
        pendingTts.clear()
        scope.coroutineContext.cancel()
    }

    fun hasMapPermission(): Boolean =
        robot.checkSelfPermission(Permission.MAP) == Permission.GRANTED

    suspend fun ensureMapPermission(timeoutMs: Long = 30_000): Boolean {
        if (hasMapPermission()) return true

        val gate = CompletableDeferred<Boolean>()
        mapPermissionGate = gate

        // Dispara el diálogo del launcher Temi (si el Manifest tiene el meta-data)
        robot.requestPermissions(listOf(Permission.MAP), REQUEST_CODE_MAP)

        return try {
            withTimeout(timeoutMs) { gate.await() }
        } catch (_: Throwable) {
            false
        } finally {
            mapPermissionGate = null
        }
    }

    override fun onRequestPermissionResult(
        permission: Permission,
        grantResult: Int,
        requestCode: Int
    ) {
        if (requestCode != REQUEST_CODE_MAP) return
        if (permission != Permission.MAP) return

        val granted = (grantResult == Permission.GRANTED)
        mapPermissionGate?.complete(granted)
    }

    private companion object {
        const val REQUEST_CODE_MAP = 1001
    }
}
