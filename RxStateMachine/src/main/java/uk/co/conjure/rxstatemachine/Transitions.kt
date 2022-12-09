package uk.co.conjure.rxstatemachine

import kotlin.reflect.KClass

/**
 * @see transitions
 */
class Transitions<A : Any, D, S>(val forAction: Map<KClass<out A>, Guard<A, D, S>> = emptyMap())


/**
 * Creates the Transition map.
 *
 * e.g.:
 * transitions(Transition(on=Action::class, guard:{_,_->nextState}))
 */
fun <A : Any, D, S> transitions(vararg transitions: Transition<A, D, S>): Transitions<A, D, S> =
    (if (transitions.isNotEmpty()) LinkedHashMap<KClass<out A>, Guard<A, D, S>>().apply {
        transitions.forEach { put(it.on, it.guard) }
    } else emptyMap()).let { Transitions(it) }


/**
 * @param on Class which kicks of the transition
 * @param guard A function that returns the new state. It will receive the current Action and Data
 * and can be used to stop the transition or decide what the next state should be.
 */
class Transition<A : Any, D, S>(val on: KClass<out A>, val guard: Guard<A, D, S>)