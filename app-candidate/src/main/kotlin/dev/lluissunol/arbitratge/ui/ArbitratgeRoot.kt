package dev.lluissunol.arbitratge.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.lluissunol.arbitratge.R

/** Raíz: la pantalla se deriva del `Stage` (03 §4.1), sin pila de navegación. */
@Composable
fun ArbitratgeRoot(vm: ExamViewModel) {
    val stage by vm.stage.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? Activity
    BackHandler { if (!vm.onBack()) activity?.finish() }

    ArbitratgeTheme {
        when (val st = stage) {
            Stage.Loading -> LoadingScreen(stringResource(R.string.recovering))
            Stage.Welcome -> WelcomeScreen(vm::onWelcomeUnderstood)
            is Stage.Home -> HomeScreen(
                st,
                onStart = vm::onStartExam,
                onShowCredential = { st.credential?.let(vm::onOpenCredential) },
                onRetryPending = vm::onRetryPending,
                onReviewLast = { st.last?.let { vm.onReview(it.sessionId, fromResult = false) } },
            )
            is Stage.Name -> NameScreen(st, vm::onNameChange, vm::onNameContinue, onBack = { vm.onBack() })
            is Stage.Rules -> RulesScreen(st, vm::onChangeName, vm::onBeginExam, onBack = { vm.onBack() })
            is Stage.Question -> QuestionScreen(st, questionActions(vm))
            is Stage.Overview -> OverviewScreen(
                st,
                questionActions(vm),
                onBack = { vm.onBack() },
                onGoTo = vm::onGoToQuestion,
                onFirstUnanswered = vm::onGoToFirstUnanswered,
                onFinish = vm::onFinishRequested,
            )
            is Stage.TimeUp -> TimeUpScreen(st, vm::onSeeResult)
            is Stage.Grading -> GradingScreen(st, vm::onRetryGrading, vm::onAbandon)
            is Stage.Result -> ResultScreen(
                st,
                onSeeCredential = { vm.onOpenCredential(it.credential, fromResult = true) },
                onRetryCredential = vm::onRetryCredential,
                onReview = { vm.onReview(st.sessionId, fromResult = true) },
                onRetry = vm::onStartExam,
                onHome = vm::onGoHome,
            )
            is Stage.Review -> ReviewScreen(st, onBack = { vm.onBack() }, onFilter = vm::onReviewFilter, onOpen = vm::onReviewOpen)
            is Stage.Credential ->
                if (st.presentation) PresentationScreen(st.credential, onClose = { vm.onPresentation(false) })
                else CredentialScreen(st.credential, onPresent = { vm.onPresentation(true) }, onHome = vm::onGoHome, onBack = { vm.onBack() })
            Stage.BuildError -> ErrorScreen(stringResource(R.string.err_build), null, null)
            Stage.RecoveryError -> ErrorScreen(stringResource(R.string.err_recover), stringResource(R.string.btn_retry), vm::onRetryRecovery)
            Stage.StateError -> ErrorScreen(stringResource(R.string.err_state), stringResource(R.string.btn_new), vm::onRecover)
        }
    }
}

private fun questionActions(vm: ExamViewModel) = QuestionActions(
    onSelect = vm::onSelect,
    onPrevious = vm::onPrevious,
    onNext = vm::onNext,
    onOverview = vm::onOpenOverview,
    onToggleTime = vm::onToggleTimeHidden,
    onDismissBanner = vm::onDismissBanner,
    onDialogConfirm = vm::onDialogConfirm,
    onDialogDismiss = vm::onDialogDismiss,
    onRetrySave = vm::onRetrySave,
    onAbandon = vm::onAbandon,
)
