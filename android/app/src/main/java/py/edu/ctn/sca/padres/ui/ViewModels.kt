package py.edu.ctn.sca.padres.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import py.edu.ctn.sca.padres.Graph
import py.edu.ctn.sca.padres.graph

/** Builds a [ViewModel] wired from the app [Graph]; no DI framework needed. */
@Composable
inline fun <reified VM : ViewModel> graphViewModel(
    crossinline create: (Graph) -> VM,
): VM {
    // El Graph sale del contexto y no de APPLICATION_KEY: SessionViewModelScope usa un
    // ViewModelStoreOwner propio sin extras por defecto, y ahí APPLICATION_KEY llega null.
    val graph = LocalContext.current.graph
    val factory: ViewModelProvider.Factory = remember(graph) {
        viewModelFactory {
            initializer { create(graph) }
        }
    }
    return viewModel(factory = factory)
}
