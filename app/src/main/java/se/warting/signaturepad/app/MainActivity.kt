package se.warting.signaturepad.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.fragment.compose.AndroidFragment
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Scaffold { padding ->

                    Navigation(modifier = Modifier
                        .padding(padding)
                        .safeContentPadding())
                }
            }
        }
    }
}


// Define the routes in your app and any arguments.
sealed class Destination {
    data object HomeDestination : Destination()
    data object ComposeDestination : Destination()
    data object DataBindingDestination : Destination()
    data object ViewDestination : Destination()
    data object SaveRestoreDestination : Destination()
}

@Composable
fun Navigation(modifier: Modifier = Modifier) {

// Create a back stack, specifying the route the app should start with.
    val backStack = remember { mutableStateListOf<Destination>(Destination.HomeDestination) }

// A NavDisplay displays your back stack. Whenever the back stack changes, the display updates.
    NavDisplay(
        modifier = modifier,
        backStack = backStack,

        // Specify what should happen when the user goes back
        onBack = { backStack.removeLastOrNull() },

        // An entry provider converts a route into a NavEntry which contains the content for that route.
        entryProvider = { route ->
            when (route) {
                is Destination.HomeDestination -> NavEntry(route) {
                    Home(
                        nav = { destination ->
                            // To navigate to a new route, just add that route to the back stack
                            backStack.add(destination)
                        }
                    )
                }

                is Destination.ComposeDestination -> NavEntry(route) {
                    ComposeSample()
                }

                Destination.DataBindingDestination -> NavEntry(route) {
                    AndroidFragment<DataBindingSampleFragment>()
                }

                Destination.SaveRestoreDestination -> NavEntry(route) {
                    SaveRestoreSample()
                }

                Destination.ViewDestination -> NavEntry(route) {
                    AndroidFragment<ViewFragment>()
                }
            }
        }
    )
}

@Composable
fun Home(
    nav: (Destination) -> Unit
) {

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .safeContentPadding()
        ) {
            Button(onClick = {
                nav(Destination.ComposeDestination)
            }) {
                Text("Compose")
            }

            Button(onClick = {
                nav(Destination.ViewDestination)
            }) {
                Text("View")
            }
            Button(onClick = {
                nav(Destination.ViewDestination)
            }) {
                Text("databind")
            }
            Button(onClick = {
                nav(Destination.SaveRestoreDestination)
            }) {
                Text("SaveRestoreActivity")
            }
        }
    }
}
