package pl.edu.ur.ar131498.wavify

enum class GestureAction {
    NEXT,
    PREVIOUS,
    PLAY_PAUSE,
    PLAY,
    PAUSE
}

class GestureController(
    private val motion: MotionGestureController,
    private val hand: HandGestureController
) {
    fun enableMotion() {
        hand.stop()
        motion.start()
    }

    fun enableHand() {
        motion.stop()
        hand.start()
    }

    fun disableAll() {
        motion.stop()
        hand.stop()
    }
}