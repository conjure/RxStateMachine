package uk.co.conjure.rxstatemachine

/**
 * An Effect can be kicked off when a State is entered and will be canceled when the state is left.
 * Use Effects to load data, send API requests etc.
 *
 * The actual implementation of the Effect is part of the StateMachine sub-class. The [Effect.create] function
 * usually will return a Sealed Class.
 *
 * @see [State.onEnter]
 * @see [StateMachine.execute]
 */
fun interface Effect<D, E> {
    /**
     * Takes the current data and returns an Effect.
     */
    fun create(data: D): E
}