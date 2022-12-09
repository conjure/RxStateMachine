package uk.co.conjure.rxstatemachine

import androidx.annotation.VisibleForTesting
import io.reactivex.rxjava3.core.*
import io.reactivex.rxjava3.subjects.PublishSubject

/**
 * The state machine implementation.
 *
 * Don't forget to subscribe to the [Completable] returned by [start] to start the state machine.
 *
 * * [state] holds the current state
 * * [data] holds the current data (across states)
 * * [reset] can be used to reset data and state to [initialData] and [initialState].
 */
abstract class StateMachine<A : Any, D : Any, S : State<D, A, E, S>, E : Any> constructor(
    /**
     * Initial state
     */
    private val initialState: S,
    /**
     * Initial data
     */
    private val initialData: D,
    /**
     * Scheduler for the StateMachine.
     * Usually the MainScheduler.
     */
    private val scheduler: Scheduler,
) {
    private lateinit var actions: Observable<out A>

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    protected val _states: PublishSubject<S> = PublishSubject.create()

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    protected val _data: PublishSubject<D> = PublishSubject.create()

    /**
     * Hot [Observable] of data changes.
     */
    val data: Observable<D> = Observable.merge(_data, Observable.just(initialData))
        .replay(1)
        .refCount()

    /**
     * Hot [Observable] of state changes
     */
    val state: Observable<S> = Observable.merge(_states, Observable.just(initialState))
        .replay(1)
        .refCount()

    /**
     * Function to implement the logic for all Effects
     */
    abstract fun execute(effect: E): Observable<A>

    private val engine =
        Completable.mergeArray(
            data.ignoreElements(),
            state.withLatestFrom(data) { state, data -> Pair(state, data) }
                .switchMapCompletable { current ->
                    val state = current.first
                    val currentData = current.second
                    observeActions(state, runStateEffect(state, currentData))
                })
            .doOnDispose { reset() }

    private fun observeActions(state: S, effect: Observable<A>) =
        Observable.merge(effect.observeOn(scheduler), actions)
            .withLatestFrom(data) { action, data -> Update(action, state, data) }
            .concatMapCompletable { update ->
                updateData(update)
                    .map { newData -> update.copy(data = newData) }
                    .flatMapCompletable { updateState(it) }
            }

    private fun runStateEffect(state: S, data: D): Observable<A> {
        val effect = state.onEnter?.create(data)
        return if (effect == null) Observable.empty()
        else return execute(effect)
    }

    private fun updateState(update: Update<A, D, S>) =
        Completable.fromAction {
            val nextState = update.state.transitions.forAction[update.action::class]?.nextState(
                update.action,
                update.data
            )
            logStateUpdate(update, nextState)
            if (nextState != null) _states.onNext(nextState)
        }

    private fun updateData(update: Update<A, D, S>) =
        Single.fromCallable {
            val newData = update.state.dataUpdate?.invoke(update.action, update.data)
            if (newData != null) {
                logDataUpdate(update.data, newData)
                _data.onNext(newData)
            }
            newData ?: update.data
        }


    /**
     * Start the StateMachine
     *
     * @param userActions an Observable of actions triggered by the user
     * @return [Completable] to manage the lifecycle
     */
    fun start(userActions: Observable<out A>): Completable {
        this.actions = userActions
        return engine
    }

    /**
     * Reset the state machine to [initialData] and [initialState]
     */
    fun reset() {
        _data.onNext(initialData)
        _states.onNext(initialState)
    }

    /**
     * Can be overwritten to log data updates
     */
    protected open fun logDataUpdate(data: D, newData: D) {}

    /**
     * Can be overwritten to log state changes
     */
    protected open fun logStateUpdate(state: Update<A, D, S>, next: S?) {}

    /**
     * Just a class to combine current action, state and data
     */
    protected data class Update<A : Any, D, S>(val action: A, val state: S, val data: D)
}