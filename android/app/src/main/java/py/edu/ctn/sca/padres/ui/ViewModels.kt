package py.edu.ctn.sca.padres.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import py.edu.ctn.sca.padres.Graph
import py.edu.ctn.sca.padres.ScaApp

/** Builds a [ViewModel] wired from the app [Graph]; no DI framework needed. */
@Composable
inline fun <reified VM : ViewModel> graphViewModel(
    crossinline create: (Graph) -> VM,
): VM {
    val factory: ViewModelProvider.Factory = remember {
        viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as ScaApp
                create(app.graph)
            }
        }
    }
    return viewModel(factory = factory)
}
