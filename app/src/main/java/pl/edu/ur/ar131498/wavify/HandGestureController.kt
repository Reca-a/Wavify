package pl.edu.ur.ar131498.wavify

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class HandGestureController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onAction: (GestureAction) -> Unit
) {

    private var cameraExecutor: ExecutorService? = null
    private var handLandmarker: HandLandmarker? = null
    private var cameraProvider: ProcessCameraProvider? = null

    private val mainHandler = Handler(Looper.getMainLooper())

    private var lastActionTime = 0L
    private val cooldownMs = 1200L

    fun start() {
        if (cameraExecutor == null || cameraExecutor?.isShutdown == true) {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }
        setupHandLandmarker()
        startCamera()
    }

    fun stop() {
        cameraProvider?.unbindAll()
        cameraExecutor?.shutdown()
        cameraExecutor = null
        handLandmarker?.close()
        handLandmarker = null
    }

    private fun setupHandLandmarker() {
        try {
            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(
                    com.google.mediapipe.tasks.core.BaseOptions.builder()
                        .setModelAssetPath("hand_landmarker.task")
                        .build()
                )
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(1)
                .setResultListener { result, _ ->
                    handleResult(result)
                }
                .setErrorListener { error ->
                }
                .build()

            handLandmarker = HandLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(context)

        providerFuture.addListener({
            try {
                cameraProvider = providerFuture.get()

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(cameraExecutor!!) { image ->
                    processImage(image)
                }

                val selector = CameraSelector.DEFAULT_FRONT_CAMERA

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    analysis
                )
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun processImage(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
        val mpImage = BitmapImageBuilder(bitmap).build()
        handLandmarker?.detectAsync(mpImage, System.currentTimeMillis())
        imageProxy.close()
    }

    private fun handleResult(result: HandLandmarkerResult) {
        if (result.landmarks().isEmpty()) return

        val landmarks = result.landmarks()[0]
        val wrist = landmarks[0]
        val indexTip = landmarks[8]
        val middleTip = landmarks[12]
        val thumbTip = landmarks[4]

        val dx = indexTip.x() - wrist.x()
        val dy = indexTip.y() - wrist.y()

        val now = System.currentTimeMillis()
        if (now - lastActionTime < cooldownMs) return

        val action = when {
//            TODO: zmienić gesty next/prev (może kciuk / wskazujący)
            // Ruch dłoni w górę -> next
            dy < -0.2f && abs(dy) > abs(dx) * 1.5f -> GestureAction.NEXT

            // Ruch dłoni w dół -> prev
            dy > 0.2f && abs(dy) > abs(dx) * 1.5f -> GestureAction.PREVIOUS

            // Otwarta dłoń - pause
            isOpenPalm(landmarks) -> GestureAction.PAUSE

            // Zaciśnięta pięść - play
            isClosedFist(landmarks) -> GestureAction.PLAY

            else -> return
        }

        lastActionTime = now
        mainHandler.post {
            onAction(action)
        }
    }

    // Otwarta dłoń - wszystkie palce wyprostowane
    private fun isOpenPalm(landmarks: List<NormalizedLandmark>): Boolean {
        val fingerTips = listOf(4, 8, 12, 16, 20) // kciuk, wskazujący, środkowy, serdeczny, mały
        val fingerBases = listOf(2, 5, 9, 13, 17) // podstawy palców

        var extended = 0
        for (i in fingerTips.indices) {
            val tip = landmarks[fingerTips[i]]
            val base = landmarks[fingerBases[i]]

            // Palec jest wyprostowany jeśli czubek jest dalej od nadgarstka niż podstawa
            if (tip.y() < base.y()) {
                extended++
            }
        }

        // Uznaj za otwartą dłoń jeśli min. 4 palce są wyprostowane
        return extended >= 4
    }

    // Zaciśnięta pięść - wszystkie palce zgięte
    private fun isClosedFist(landmarks: List<NormalizedLandmark>): Boolean {
        val fingerTips = listOf(8, 12, 16, 20) // bez kciuka
        val wrist = landmarks[0]
        val palmCenter = landmarks[9] // środek dłoni

        var folded = 0
        for (tipIndex in fingerTips) {
            val tip = landmarks[tipIndex]

            // Palec jest zgięty jeśli czubek jest blisko środka dłoni
            val distance = abs(tip.y() - palmCenter.y())
            if (distance < 0.1f) {
                folded++
            }
        }

        // Uznaj za pięść jeśli wszystkie 4 palce są zgięte
        return folded >= 3
    }
}
