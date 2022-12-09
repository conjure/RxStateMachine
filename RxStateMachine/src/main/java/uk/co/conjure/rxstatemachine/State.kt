package uk.co.conjure.rxstatemachine

/**
 * A State in the StateMachine.
 *
 * When a state is entered it will first trigger the Effect [onEnter]. Then it will listen to any
 * Actions in [transitions], perform [dataUpdate] and transition to the
 * next state. When transitioning to a new state any ongoing Effect from [onEnter] will be canceled.
 */
interface State<D, A : Any, E, S> {
    val transitions: Transitions<A, D, S>
    val dataUpdate: ((A, D) -> D?)?
    val onEnter: Effect<D, E>?
}