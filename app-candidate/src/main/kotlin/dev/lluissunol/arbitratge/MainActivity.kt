package dev.lluissunol.arbitratge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.lluissunol.arbitratge.ui.ArbitratgeRoot
import dev.lluissunol.arbitratge.ui.ExamViewModel
import dev.lluissunol.arbitratge.ui.UiEvent
import kotlinx.coroutines.launch

/** Única Activity (03 §4.1): estado en BD + `StateFlow`; la rotación recrea la Activity sin perder nada. */
class MainActivity : ComponentActivity() {
    private val vm: ExamViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ExamViewModel((application as ArbitratgeApp).container.examDeps()) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ArbitratgeRoot(vm) }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.events.collect { if (it == UiEvent.LeaveApp) finish() }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        vm.onAppStarted()
    }

    override fun onStop() {
        vm.onAppStopped()
        super.onStop()
    }
}
