package uk.co.conjure.rxstatemachine

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.observers.TestObserver
import io.reactivex.rxjava3.schedulers.TestScheduler
import io.reactivex.rxjava3.subjects.PublishSubject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test


class StateMachineTests {

    private lateinit var uut: TestStateMachine

    private lateinit var scheduler: TestScheduler

    private val actionSubject: PublishSubject<TestAction> = PublishSubject.create()
    private val effectSubject: PublishSubject<TestAction> = PublishSubject.create()

    private lateinit var subscription: TestObserver<Void>

    private var effects = emptyList<TestEffect>()

    @Before
    fun setUp() {
        scheduler = TestScheduler()
        uut = object : TestStateMachine(scheduler) {
            override fun execute(effect: TestEffect): Observable<TestAction> {
                effects = effects.plus(effect)
                return effectSubject
            }
        }
        subscription = uut.start(actionSubject).test()
    }

    @After
    fun tearDown() {
        subscription.dispose()
    }

    @Test
    fun `Data emits initialData`() {
        uut.data.test().assertValue { it.count == 0 }.dispose()
    }

    @Test
    fun `Data emits data updates`() {
        val test = uut.data.test()

        actionSubject.onNext(TestAction.Increment)

        test.awaitCount(2)
            .assertValueCount(2)
            .assertValueAt(1) { it.count == 1 }
            .dispose()
    }

    @Test
    fun `State emits initialState`() {
        uut.state.test().assertValue { it is TestState.FirstState }.dispose()
    }

    @Test
    fun `State emits state updates`() {
        val test = uut.state.test()

        actionSubject.onNext(TestAction.IncrementAsync)

        test
            .assertValueCount(2)
            .assertValueAt(1) { it is TestState.AsyncState }
            .dispose()
    }

    @Test
    fun `Effect triggered`() {
        assertEquals("No Effect should have been triggered yet!", 0, effects.size)
        actionSubject.onNext(TestAction.IncrementAsync)
        assertEquals("No Effect should have been triggered yet!", 1, effects.size)
        assertEquals(TestEffect.Increment, effects[0])
    }


    @Test
    fun `Effect result moves to next state when scheduler triggers actions`() {
        val test = uut.state.test()
            .assertValue { it is TestState.FirstState }

        actionSubject.onNext(TestAction.IncrementAsync)
        effectSubject.onNext(TestAction.AsyncResponse.IncrementComplete)

        test.assertValueAt(1) { it is TestState.AsyncState }

        scheduler.triggerActions()

        test.assertValueCount(3)
            .assertValueAt(2) { it is TestState.FirstState }
            .dispose()
    }


}