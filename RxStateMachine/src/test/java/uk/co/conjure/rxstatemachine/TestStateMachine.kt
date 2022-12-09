package uk.co.conjure.rxstatemachine

import io.reactivex.rxjava3.core.Scheduler
import uk.co.conjure.rxstatemachine.TestAction.*

sealed class TestAction {
    object Increment : TestAction()
    object Decrement : TestAction()

    object IncrementAsync : TestAction()

    sealed class AsyncResponse : TestAction() {
        object IncrementComplete : AsyncResponse()
    }
}

data class TestData(val count: Int) {
    fun increment(): TestData {
        return copy(count = count + 1)
    }

    fun decrement(): TestData {
        return copy(count = count - 1)
    }
}

sealed class TestEffect {
    object Increment : TestEffect()
}

sealed class TestState(
    override val transitions: Transitions<TestAction, TestData, TestState> = transitions(),
    override val onEnter: Effect<TestData, TestEffect>? = null,
    override val dataUpdate: ((TestAction, TestData) -> TestData?)? = null
) : State<TestData, TestAction, TestEffect, TestState> {

    object FirstState : TestState(
        transitions = transitions(
            Transition(on = IncrementAsync::class) { _, _ -> AsyncState }
        ),
        dataUpdate = { action, data ->
            when (action) {
                is Increment -> data.increment()
                is Decrement -> data.decrement()
                else -> null
            }
        },
    )

    object AsyncState : TestState(
        transitions = transitions(
            Transition(on = AsyncResponse.IncrementComplete::class) { _, _ -> FirstState }
        ),
        dataUpdate = { action, data ->
            if (action is AsyncResponse.IncrementComplete) data.increment() else null
        },
        onEnter = { TestEffect.Increment }
    )
}

abstract class TestStateMachine(scheduler: Scheduler) :
    StateMachine<TestAction, TestData, TestState, TestEffect>(
        initialState = TestState.FirstState,
        initialData = TestData(0),
        scheduler = scheduler
    ) {

    fun setData(data: TestData) {
        _data.onNext(data)
    }

    fun setState(state: TestState) {
        _states.onNext(state)
    }
}