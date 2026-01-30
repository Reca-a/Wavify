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
import kotlin.math.pow
import kotlin.math.sqrt

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

    // Bufor do stabilizacji gestów
    private val gestureBuffer = mutableListOf<GestureAction?>()
    private val bufferSize = 3

    private var lastGestureType: GestureAction? = null

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
        gestureBuffer.clear()
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
                .setMinHandDetectionConfidence(0.6f)
                .setMinHandPresenceConfidence(0.6f)
                .setMinTrackingConfidence(0.5f)
                .setResultListener { result, _ ->
                    handleResult(result)
                }
                .setErrorListener { _ ->
                }
                .build()

            handLandmarker = HandLandmarker.createFromOptions(context, options)
        } catch (_: Exception) {
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
        if (result.landmarks().isEmpty()) {
            gestureBuffer.clear()
            return
        }

        val landmarks = result.landmarks()[0]
        val detectedGesture = detectGesture(landmarks)

        // Dodaj do bufora
        gestureBuffer.add(detectedGesture)
        if (gestureBuffer.size > bufferSize) {
            gestureBuffer.removeAt(0)
        }

        // Sprawdź czy gest jest stabilny
        val stableGesture = getStableGesture()

        if (stableGesture == GestureAction.NEXT || stableGesture == GestureAction.PREVIOUS)
            lastGestureType = null

        if (stableGesture != null && stableGesture != lastGestureType) {
            val now = System.currentTimeMillis()

            if (now - lastActionTime >= cooldownMs) {
                lastActionTime = now
                lastGestureType = stableGesture

                mainHandler.post {
                    onAction(stableGesture)
                }
            }
        }
    }

    private fun detectGesture(landmarks: List<NormalizedLandmark>): GestureAction? {
        // Sprawdź gesety w kolejności priorytetów
        return when {
            isOneFinger(landmarks) -> GestureAction.NEXT
            isThreeFingers(landmarks) -> GestureAction.PREVIOUS
            isClosedFist(landmarks) -> GestureAction.PLAY
            isOpenPalm(landmarks) -> GestureAction.PAUSE
            else -> null
        }
    }

    private fun getStableGesture(): GestureAction? {
        if (gestureBuffer.isEmpty()) return null

        val gestureCounts = gestureBuffer.filterNotNull().groupingBy { it }.eachCount()
        val mostCommon = gestureCounts.maxByOrNull { it.value }

        return if (mostCommon != null && mostCommon.value >= (bufferSize * 0.5).toInt()) {
            mostCommon.key
        } else {
            null
        }
    }

    // Jeden palec (wskazujący) - next
    private fun isOneFinger(landmarks: List<NormalizedLandmark>): Boolean {
        val wrist = landmarks[0]
        val thumbTip = landmarks[4]
        val indexTip = landmarks[8]
        val middleTip = landmarks[12]
        val ringTip = landmarks[16]
        val pinkyTip = landmarks[20]

        val indexBase = landmarks[5]
        val middleBase = landmarks[9]
        val ringBase = landmarks[13]
        val pinkyBase = landmarks[17]

        // Palec wskazujący wyprostowany (czubek dalej od nadgarstka niż podstawa)
        val indexDist = sqrt((indexTip.x() - wrist.x()).pow(2) + (indexTip.y() - wrist.y()).pow(2))
        val indexBaseDist = sqrt((indexBase.x() - wrist.x()).pow(2) + (indexBase.y() - wrist.y()).pow(2))
        val indexExtended = indexDist > indexBaseDist + 0.08f

        // Środkowy palec zgięty
        val middleDist = sqrt((middleTip.x() - wrist.x()).pow(2) + (middleTip.y() - wrist.y()).pow(2))
        val middleBaseDist = sqrt((middleBase.x() - wrist.x()).pow(2) + (middleBase.y() - wrist.y()).pow(2))
        val middleFolded = middleDist < middleBaseDist + 0.03f

        // Serdeczny zgięty
        val ringDist = sqrt((ringTip.x() - wrist.x()).pow(2) + (ringTip.y() - wrist.y()).pow(2))
        val ringBaseDist = sqrt((ringBase.x() - wrist.x()).pow(2) + (ringBase.y() - wrist.y()).pow(2))
        val ringFolded = ringDist < ringBaseDist + 0.03f

        // Mały palec zgięty
        val pinkyDist = sqrt((pinkyTip.x() - wrist.x()).pow(2) + (pinkyTip.y() - wrist.y()).pow(2))
        val pinkyBaseDist = sqrt((pinkyBase.x() - wrist.x()).pow(2) + (pinkyBase.y() - wrist.y()).pow(2))
        val pinkyFolded = pinkyDist < pinkyBaseDist + 0.03f

        return indexExtended && middleFolded && ringFolded && pinkyFolded
    }

    // Trzy palce (wskazujący, środkowy, serdeczny) - prev
    private fun isThreeFingers(landmarks: List<NormalizedLandmark>): Boolean {
        val wrist = landmarks[0]
        val indexTip = landmarks[8]
        val middleTip = landmarks[12]
        val ringTip = landmarks[16]
        val pinkyTip = landmarks[20]

        val indexBase = landmarks[5]
        val middleBase = landmarks[9]
        val ringBase = landmarks[13]
        val pinkyBase = landmarks[17]

        // Wskazujący wyprostowany
        val indexDist = sqrt((indexTip.x() - wrist.x()).pow(2) + (indexTip.y() - wrist.y()).pow(2))
        val indexBaseDist = sqrt((indexBase.x() - wrist.x()).pow(2) + (indexBase.y() - wrist.y()).pow(2))
        val indexExtended = indexDist > indexBaseDist + 0.08f

        // Środkowy wyprostowany
        val middleDist = sqrt((middleTip.x() - wrist.x()).pow(2) + (middleTip.y() - wrist.y()).pow(2))
        val middleBaseDist = sqrt((middleBase.x() - wrist.x()).pow(2) + (middleBase.y() - wrist.y()).pow(2))
        val middleExtended = middleDist > middleBaseDist + 0.08f

        // Serdeczny wyprostowany
        val ringDist = sqrt((ringTip.x() - wrist.x()).pow(2) + (ringTip.y() - wrist.y()).pow(2))
        val ringBaseDist = sqrt((ringBase.x() - wrist.x()).pow(2) + (ringBase.y() - wrist.y()).pow(2))
        val ringExtended = ringDist > ringBaseDist + 0.08f

        // Mały palec zgięty
        val pinkyDist = sqrt((pinkyTip.x() - wrist.x()).pow(2) + (pinkyTip.y() - wrist.y()).pow(2))
        val pinkyBaseDist = sqrt((pinkyBase.x() - wrist.x()).pow(2) + (pinkyBase.y() - wrist.y()).pow(2))
        val pinkyFolded = pinkyDist < pinkyBaseDist + 0.03f

        return indexExtended && middleExtended && ringExtended && pinkyFolded
    }

    // Otwarta dłoń (wszystkie 5 palców) - pause
    private fun isOpenPalm(landmarks: List<NormalizedLandmark>): Boolean {
        val wrist = landmarks[0]
        val fingerTips = listOf(4, 8, 12, 16, 20)
        val fingerBases = listOf(2, 5, 9, 13, 17)

        var extended = 0
        for (i in fingerTips.indices) {
            val tip = landmarks[fingerTips[i]]
            val base = landmarks[fingerBases[i]]

            // Oblicz odległość od nadgarstka
            val tipDist = sqrt((tip.x() - wrist.x()).pow(2) + (tip.y() - wrist.y()).pow(2))
            val baseDist = sqrt((base.x() - wrist.x()).pow(2) + (base.y() - wrist.y()).pow(2))

            // Palec jest wyprostowany jeśli czubek jest wyraźnie dalej
            if (tipDist > baseDist + 0.06f) {
                extended++
            }
        }

        return extended >= 5  // Wszystkie 5 palców muszą być wyprostowane
    }

    // Zaciśnięta pięść (0 palców) - play
    private fun isClosedFist(landmarks: List<NormalizedLandmark>): Boolean {
        val wrist = landmarks[0]
        val palmCenter = landmarks[9]
        val fingerTips = listOf(4, 8, 12, 16, 20)

        // Sprawdź czy wszystkie czubki palców są blisko środka dłoni
        var closeToCenter = 0
        for (tipIndex in fingerTips) {
            val tip = landmarks[tipIndex]
            val distance = sqrt(
                (tip.x() - palmCenter.x()).pow(2) +
                        (tip.y() - palmCenter.y()).pow(2)
            )

            if (distance < 0.12f) {
                closeToCenter++
            }
        }

        // Pięść = wszystkie palce blisko centrum
        return closeToCenter >= 4
    }
}