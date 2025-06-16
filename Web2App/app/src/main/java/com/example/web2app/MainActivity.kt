package com.example.web2app

import android.app.DownloadManager
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.web2app.ui.theme.Web2AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONObject
import androidx.core.net.toUri

// --- Screen Routes ---
object Routes {
    const val OPENING_SCREEN = "opening_screen"
    const val INPUT_URL_SCREEN = "input_url_screen"
    const val RESULT_SCREEN = "result_screen/{generatedUrl}" // Pass URL as an Argument
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Web2AppTheme {
                // Surface is typically used as the root for your app's content
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Routes.OPENING_SCREEN
                    ) {
                        composable(Routes.OPENING_SCREEN) {
                            OpeningScreen(navController = navController)
                        }
                        composable(Routes.INPUT_URL_SCREEN) {
                            InputUrlScreen(navController = navController)
                        }
                        composable(Routes.RESULT_SCREEN + "/{generatedUrl}") { backStackEntry ->
                            val generatedUrl = backStackEntry.arguments?.getString("generatedUrl") ?: "N/A"
                            ResultScreen(navController = navController, generatedUrl = generatedUrl)
                        }
                    }
                }
            }
        }
    }
}

// --- 1. Opening Screen ---
@Composable
fun OpeningScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.web2app0comp), // Replace with your image name
            contentDescription = "Web2App Logo", // Important for Accessibility
            modifier = Modifier
                .size(250.dp) // Set a desired size for the image
                .padding(bottom = 24.dp) // Add some padding below the image
        )
        Text(
            text = "Welcome to Web2App!",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Text(
            text = "Easily turn any Website into a Standalone Android App.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 48.dp)
        )
        Button(
            onClick = { navController.navigate(Routes.INPUT_URL_SCREEN) },
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(50.dp)
        ) {
            Text(
                text = "Get Started",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

// --- 2. Input URL Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputUrlScreen(navController: NavController) {
    var websiteUrl by remember { mutableStateOf("https://www.google.com") } // Pre-fill for Testing
    var statusMessage by remember { mutableStateOf("Enter a Website URL to generate app.") }
    var isLoading by remember { mutableStateOf(false) } // New State of Loading Indicator

    val context = LocalContext.current //Get Context
    val client = remember { OkHttpClient() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Generate Web App APK",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = websiteUrl,
            onValueChange = { websiteUrl = it },
            label = { Text("Website URL (e.g., https://example.com)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading // Disable input when loading
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (websiteUrl.isNotBlank() && (websiteUrl.startsWith("http://") || websiteUrl.startsWith("https://"))) {
                    isLoading = true //Start Loading
                    statusMessage = "Sending Request to Server for: $websiteUrl..."

                    // --- IMPORTANT: This is where you'd trigger the actual generation process ---
                    // For UI demonstration, we'll navigate to the result screen immediately.
                    // In a real app, you'd perform the generation, await its result,
                    // and *then* navigate to the ResultScreen with the actual outcome.

                    //Make the network request in a coroutine
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val jsonBody = JSONObject().apply { put("url", websiteUrl) }.toString()
                            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                            val request = okhttp3.Request.Builder()
                                .url("http://<YOUR_SERVER_IP>:5000/generate-apk") // <-- IMPORTANT: REPLACE WITH YOUR SERVER'S IP = YOUR_SERVER_IP
                                .post(requestBody)
                                .build()

                            val response = client.newCall(request).execute()
                            val responseBody = response.body?.string()

                            if (response.isSuccessful && responseBody != null) {
                                val jsonResponse = JSONObject(responseBody)
                                if (jsonResponse.getBoolean("success")) {
                                    val downloadUrl = jsonResponse.getString("download_url")
                                    //Navigate to result screen on UI thread
                                    with(Dispatchers.Main) {
                                        navController.navigate("${Routes.RESULT_SCREEN}/${downloadUrl.encodeURL()}")
                                    }
                                } else {
                                    with(Dispatchers.Main) {
                                        statusMessage = "Server Error: ${jsonResponse.optString("message", "Unknown error")}"
                                    }
                                }
                            } else {
                                with(Dispatchers.Main) {
                                    statusMessage = "HTTP Error: ${response.code} - ${response.message}"
                                }
                            }
                        } catch (e: IOException) {
                            with(Dispatchers.Main) {
                                statusMessage = "Network Error: ${e.message}"
                            }
                            e.printStackTrace()
                        } finally {
                            with(Dispatchers.Main) {
                                isLoading = false // Stop loading
                            }
                        }
                    }
                } else if (websiteUrl.isBlank()){
                    Toast.makeText(context, "URL cannot be empty", Toast.LENGTH_SHORT).show()
                } else if (!Patterns.WEB_URL.matcher(websiteUrl).matches()) {
                    Toast.makeText(context, "Invalid URL", Toast.LENGTH_SHORT).show()
                    statusMessage = "Please enter a valid URL"
                } else {
                    statusMessage = "Please enter a valid URL (starting with http:// or https://)."
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading //Disable button when loading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
                Text("Generating...")
            } else {
                Text(
                            text = "Generate Web App",
                            style = MaterialTheme.typography.titleMedium
                 )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

// Helper function to encode URL for navigation (important for special characters)
fun String.encodeURL(): String {
    return java.net.URLEncoder.encode(this, "UTF-8")
}

// --- 3. Result Screen ---
@Composable
fun ResultScreen(navController: NavController, generatedUrl: String) {
    val context = LocalContext.current // Get the context for DownloadManager

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Generation Complete!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Web App generated for:",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = generatedUrl,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 32.dp),
            color = MaterialTheme.colorScheme.secondary
        )

        Button(
            onClick = {
                // TODO: Implement actual APK download logic here
                // This would likely involve starting a download manager,
                // opening a browser to a download link, or handling a received file.
                // For now, let's just update a message.

                //Initiate Download using DownloadManager
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val uri = generatedUrl.toUri()
                val request = DownloadManager.Request(uri)
                    .setMimeType("application/vnd.android.package-archive") // Important for APKs
                    .setTitle("Web2App APK") //Title shown in notifications
                    .setDescription("Downloading your generated Web App.")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "webapp_${System.currentTimeMillis()}.apk") // Save to Downloads Folder

                try {
                    downloadManager.enqueue(request)
                    Toast.makeText(context, "Download started!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            },
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(50.dp)
        ) {
            Text("Download APK")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = { navController.popBackStack(Routes.INPUT_URL_SCREEN, inclusive = false) }, // Go back to input screen
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Text("Generate Next")
        }

    }
}


// --- Previews ---
@Preview(showBackground = true)
@Composable
fun PreviewOpeningScreen() {
    Web2AppTheme {
        OpeningScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewInputUrlScreen() {
    Web2AppTheme {
        InputUrlScreen(navController = rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewResultScreen() {
    Web2AppTheme {
        ResultScreen(navController = rememberNavController(), generatedUrl = "https://example.com/myapp.apk")
    }
}