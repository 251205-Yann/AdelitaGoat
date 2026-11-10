package mx.edu.upchiapas.adelitagoat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import mx.edu.upchiapas.adelitagoat.data.AdelitaRepository
import mx.edu.upchiapas.adelitagoat.data.SessionStore
import mx.edu.upchiapas.adelitagoat.ui.navigation.AdelitaNavHost
import mx.edu.upchiapas.adelitagoat.ui.theme.AdelitaGOATTheme
import mx.edu.upchiapas.adelitagoat.ui.viewmodel.AdelitaViewModelFactory
import mx.edu.upchiapas.adelitagoat.ui.viewmodel.AuthViewModel
import mx.edu.upchiapas.adelitagoat.ui.viewmodel.CustomerViewModel
import mx.edu.upchiapas.adelitagoat.ui.viewmodel.VendorViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionStore = SessionStore(applicationContext)
        val repository = AdelitaRepository(sessionStore)
        val factory = AdelitaViewModelFactory(repository)

        setContent {
            AdelitaGOATTheme {
                val authViewModel: AuthViewModel = viewModel(factory = factory)
                val customerViewModel: CustomerViewModel = viewModel(factory = factory)
                val vendorViewModel: VendorViewModel = viewModel(factory = factory)

                AdelitaNavHost(
                    authViewModel = authViewModel,
                    customerViewModel = customerViewModel,
                    vendorViewModel = vendorViewModel
                )
            }
        }
    }
}
