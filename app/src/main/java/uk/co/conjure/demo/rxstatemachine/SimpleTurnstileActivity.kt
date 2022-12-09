package uk.co.conjure.demo.rxstatemachine

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.subjects.PublishSubject
import uk.co.conjure.demo.rxstatemachine.databinding.ActivitySimpleTurnstileBinding

/**
 * Simulates a turnstile with two buttons "Insert Coin" and "Turn".
 * The ViewModel is backed by the [SimpleTurnstileStateMachine]
 */
class SimpleTurnstileActivity : AppCompatActivity() {

    private val viewModel: SimpleTurnstileViewModel by viewModels()
    private val subscriptions = CompositeDisposable()
    private lateinit var binding: ActivitySimpleTurnstileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySimpleTurnstileBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        bindView(viewModel)
    }

    override fun onStop() {
        subscriptions.clear()
        super.onStop()
    }

    override fun onDestroy() {
        subscriptions.dispose()
        super.onDestroy()
    }

    private fun bindView(vm: TurnstileViewModel) {
        with(vm) {
            // Bind click events
            binding.btnInsertCoin.setOnClickListener { insertCoin.onNext(Unit) }
            binding.btnTurn.setOnClickListener { turn.onNext(Unit) }

            // Subscribe to the State
            subscriptions.add(isUnlocked.subscribe { unlocked ->
                // Update buttons
                binding.btnInsertCoin.isEnabled = !unlocked
                binding.btnTurn.isEnabled = unlocked
                // Update text
                binding.tvState.text =
                    getString(if (unlocked) R.string.unlocked else R.string.locked)
            })
        }
    }
}

/**
 * Interface for the View
 */
interface TurnstileViewModel {
    val insertCoin: Observer<Unit>
    val turn: Observer<Unit>

    val isUnlocked: Observable<Boolean>
}


/**
 * ViewModel implementing [TurnstileViewModel] via [SimpleTurnstileStateMachine]
 */
class SimpleTurnstileViewModel : ViewModel(), TurnstileViewModel {

    /**
     * CompositeDisposable to dispose all subscriptions in the ViewModels onCleared method.
     */
    private val subscriptions = CompositeDisposable()

    /**
     * The State Machine
     */
    private val state = SimpleTurnstileStateMachine()


    override val insertCoin: PublishSubject<Unit> = PublishSubject.create()
    override val turn: PublishSubject<Unit> = PublishSubject.create()

    override val isUnlocked: Observable<Boolean> =
        state.state.map { it is SimpleTurnstileState.Unlocked }


    /**
     * When initialized start the state machine.
     * The state machine will be running until onCleared is called.
     */
    init {
        subscriptions.add(
            state.start(Observable.merge(
                insertCoin.map { SimpleTurnstileAction.InsertCoin },
                turn.map { SimpleTurnstileAction.Turn }
            ))
                .subscribe()
        )
    }

    override fun onCleared() {
        subscriptions.dispose()
        super.onCleared()
    }
}

