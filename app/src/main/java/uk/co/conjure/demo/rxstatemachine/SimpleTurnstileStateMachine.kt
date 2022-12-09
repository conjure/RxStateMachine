package uk.co.conjure.demo.rxstatemachine

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import uk.co.conjure.demo.rxstatemachine.SimpleTurnstileAction.*
import uk.co.conjure.rxstatemachine.*
import java.util.concurrent.locks.Lock


/**
 * Actions the user can perform on the turnstile.
 * He can either insert a coin, or turn the turnstile.
 * The actual behaviour is implemented in the states ([SimpleTurnstileState]).
 */
sealed class SimpleTurnstileAction {
    object InsertCoin : SimpleTurnstileAction()
    object Turn : SimpleTurnstileAction()
}

/**
 * We just keep track of how many people have entered.
 */
data class SimpleTurnstileData(val turnCount: Int)

/**
 * No Effects
 */
sealed class SimpleTurnstileEffect

/**
 * States are defining the actual behaviour of the StateMachine.
 * This state machine has 2 states [Locked] and [Unlocked].
 */
sealed class SimpleTurnstileState(
    override val transitions: Transitions<SimpleTurnstileAction, SimpleTurnstileData, SimpleTurnstileState> = transitions(),
    override val dataUpdate: ((SimpleTurnstileAction, SimpleTurnstileData) -> SimpleTurnstileData?)? = null,
    override val onEnter: Effect<SimpleTurnstileData, SimpleTurnstileEffect>? = null
) : State<SimpleTurnstileData, SimpleTurnstileAction, SimpleTurnstileEffect, SimpleTurnstileState> {

    /**
     * Wait for an Action [InsertCoin] and transition to [Unlocked].
     */
    object Locked : SimpleTurnstileState(
        transitions = transitions(
            Transition(on = InsertCoin::class) { _, _ -> Unlocked }
        )
    )

    /**
     * On the Action [Turn] we increment the turnCount and transition to [Locked].
     */
    object Unlocked : SimpleTurnstileState(
        transitions = transitions(
            Transition(on = Turn::class) { _, _ -> Locked }
        ),
        dataUpdate = { action, data ->
            if (action is Turn)
                data.copy(turnCount = data.turnCount + 1)
            else null
        }
    )
}

class SimpleTurnstileStateMachine :
    StateMachine<SimpleTurnstileAction, SimpleTurnstileData, SimpleTurnstileState, SimpleTurnstileEffect>(
        initialData = SimpleTurnstileData(0),
        initialState = SimpleTurnstileState.Locked,
        scheduler = AndroidSchedulers.mainThread()
    ) {
    override fun execute(effect: SimpleTurnstileEffect): Observable<SimpleTurnstileAction> {
        return Observable.never()
    }
}
