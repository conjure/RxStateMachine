# RxStateMachine

An RxJava based state machine.

## Concept

### States, Actions, Data and Transitions

The main concepts are `State`, `Actions`, `Data` and `Transitions`.

Where `Actions` are performed on a `State` and can trigger data updates and/or transitions.

`Transitions` move the `State` from one State to another.

`Data` is preserved and shared across `States`.

### Effects

The last piece to complete the state machine are `Effects`. `Effects` can be kicked off when
a `State` is entered and are stopped when transitioning to a new `State` (or the effect completes).

An example for an `Effect` is a state "Loading" which when entered triggers the "SubmitData"
effect.
`Effects` always return an `Observable<Action>`. It can, but doesn't have to emit items. In our
example of "SubmitData" it could emit the Action "DataUploaded" to allow the State to transition to
the next State.

It could also return multiple Actions to update a progress e.g. "DataUploading(0.2)"
, "DataUploading(0.8)", "DataUploaded".

## Example (Turnstile state machine)

The most basic example for a state machine is probably a turnstile. When the user inserts a coin it
gets unblocked and he can pass through.


<img src="https://upload.wikimedia.org/wikipedia/commons/9/97/Torniqueterevolution.jpg" alt="Turnstile" width="160"/>
<img src="https://upload.wikimedia.org/wikipedia/commons/9/9e/Turnstile_state_machine_colored.svg" alt="Turnstile state machine" width="640"/>

Image from

* <a href="https://commons.wikimedia.org/wiki/File:Torniqueterevolution.jpg">Sebasgui</a>
  , <a href="https://creativecommons.org/licenses/by-sa/4.0">CC BY-SA 4.0</a>, via Wikimedia Commons
* <a href="https://commons.wikimedia.org/wiki/File:Turnstile_state_machine_colored.svg">
  Chetvorno</a>, CC0, via Wikimedia Commons

### Actions

There is only 2 actions the user can perform

```kotlin
sealed class SimpleTurnstileAction {
    object InsertCoin : SimpleTurnstileAction()
    object Turn : SimpleTurnstileAction()
}
```

### Data

Actually there is no data required - but to make it a bit more interesting let's keep a counter how
many people have entered

```kotlin
data class SimpleTurnstileData(val turnCount: Int)
```

### Effect

Ok - there is relly no use for an effect here. So let's just make it an empty sealed class

```kotlin
sealed class SimpleTurnstileEffect
```

### State

Now this is where all the magic happens.

```kotlin
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
```

### State Machine

Now that all our behaviour is defined we are good to go and can create our StateMachine

```kotlin
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
```

And that's it. The state machine is started by subscribing to the `Completable` returned from
the `start` function.

The `state` and `data` are available on the StateMachine as hot Observables.

Check out the examples in the /app folder of the repository!