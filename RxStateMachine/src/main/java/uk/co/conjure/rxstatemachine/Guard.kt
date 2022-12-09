package uk.co.conjure.rxstatemachine

/**
 * The Guard function actually defines the transition.
 * It takes and Action and the current Data to decide what the next State should be.
 *
 * Return Null if you do not want to change the state.
 *
 * Returning the same State again is also valid, but keep in mind that this is a "state change".
 * Therefore old Effects are canceled and new Effects are triggered.
 */
fun interface Guard<A, D, S> {
    fun nextState(action: A, data: D): S?
}