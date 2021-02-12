package bogomolov.aa.anochat.features.shared.mvi

import bogomolov.aa.anochat.repository.Repository

abstract class RepositoryBaseViewModel<S : UiState>(private val repository: Repository) :
    BaseViewModel<S, DefaultContext<S>>() {
    override fun createViewModelContext() = DefaultContext(this, repository)
}

abstract class DefaultUserAction<S : UiState> : UserAction<DefaultContext<S>>

open class DefaultActionContext<V>(
    val viewModel: V,
    val repository: Repository
) : ActionContext

class DefaultContext<S : UiState>(
    viewModel: RepositoryBaseViewModel<S>,
    repository: Repository
) : DefaultActionContext<RepositoryBaseViewModel<S>>(viewModel, repository)

