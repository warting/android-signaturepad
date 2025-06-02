package se.warting.signaturepad.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) { innerPadding ->

                    val context = LocalContext.current

                    Column(Modifier.padding(innerPadding)) {
                        Button(onClick = {
                            val intent = Intent(context, ComposeActivity::class.java)
                            startActivity(intent)
                        }) {
                            Text("Compose")
                        }

                        Button(onClick = {
                            val intent = Intent(context, ViewActivity::class.java)
                            startActivity(intent)
                        }) {
                            Text("View")
                        }
                        Button(onClick = {
                            val intent = Intent(context, ViewActivity::class.java)
                            startActivity(intent)
                        }) {
                            Text("databind")
                        }
                        Button(onClick = {
                            val intent = Intent(context, SaveRestoreActivity::class.java)
                            startActivity(intent)
                        }) {
                            Text("SaveRestoreActivity")
                        }
                    }
                }

            }
        }
    }
}
