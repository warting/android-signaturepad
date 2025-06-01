package se.warting.signaturepad.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {

                    val context = LocalContext.current

                    Column {
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
