package com.template.webviewtemplate01

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    // IMPORTANT: This is the URL placeholder the server will replace!
    // Make sure this string is unique and won't appear elsewhere accidentally.
    private val TARGET_WEB_URL_PLACEHOLDER = "___YOUR_DYNAMIC_WEBVIEW_URL_PLACEHOLDER___"

    private lateinit var webView: WebView

    // Declare the callback at the class level
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    @SuppressLint("SetJavaScriptEnabled") // Suppress warning, as JS is often needed for web views
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.webView)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        webView = findViewById(R.id.webView)

        // Configure WebView settings
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true // Enable JavaScript
        webSettings.domStorageEnabled = true // Enable DOM Storage (for some web apps)
        webSettings.loadWithOverviewMode = true // Zoom out if content is wider than screen
        webSettings.useWideViewPort = true // Enable viewport meta tag
		
		// Security recommendations
		webSettings.allowFileAccess = false // Disallow file system access by default
		webSettings.allowContentAccess = false // Disallow content provider access

		// Enable or disable support for the "viewport" HTML meta tag
		webSettings.useWideViewPort = true
		webSettings.loadWithOverviewMode = true

		// Configure caching
		webSettings.cacheMode = WebSettings.LOAD_DEFAULT // Use default caching policy
		webSettings.setAppCacheEnabled(true)
		webSettings.setAppCachePath(context.cacheDir.path) // Set app cache path

        // Set a WebViewClient to handle page navigation within the WebView itself
        webView.webViewClient = object : WebViewClient() {
            // Optional: Override shouldOverrideUrlLoading if you want to handle specific URLs outside
            // the WebView (e.g., opening external links in a browser)
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
				    // Load HTTP/HTTPS URLs within the webView
                    return false // Let WebView handle the URL
                } else {
					// Handle non-web URLs (e.g., mailto:, tel:) by opening them in appropriate apps
					try {
						val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
						startActivity(intent)
					} catch (e: Exception) {
						// Handle cases where no app can handle the URL (e.g., malformed URL, no tel app)
						Toast.makeText(this@MainActivity, "Cannot open: $url", Toast.LENGTH_SHORT).show()
					}
					return true // Indicate that we handled the URL
				}
            }
			
			override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
				super.onReceivedError(view, request, error)
				// This is a basic error handler. For production, you'd show a user-friendly error page.
				if (request?.isForMainFrame == true) {
					// Only show error for the main page load
					Toast.makeText(this@MainActivity, "Error loading web page: ${error?.description}", Toast.LENGTH_LONG).show()
					// Optionally load a local error HTML page: view?.loadUrl("file:///android_asset/error.html")
				}
			}

			override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
				super.onPageStarted(view, url, favicon)
				// Show a progress indicator here (e.g., a ProgressBar)
				// progressBar.visibility = View.VISIBLE
			}

			override fun onPageFinished(view: WebView?, url: String?) {
				super.onPageFinished(view, url)
				// Hide the progress indicator here
				// progressBar.visibility = View.GONE
			}
	
        }

        // Load the URL
        // Server will replace TARGET_WEB_URL_PLACEHOLDER with the actual URL
        if (TARGET_WEB_URL_PLACEHOLDER == "___YOUR_DYNAMIC_WEBVIEW_URL_PLACEHOLDER___") {
            // This case should ideally not happen in a generated app
            // but is here for template safety / initial testing
            webView.loadUrl("about:blank") // Load a blank page or default if not replaced
        } else {
            webView.loadUrl(TARGET_WEB_URL_PLACEHOLDER)
        }

        // Handle back button press to navigate within the WebView history
        // --- New way to handle back press ---
        onBackPressedCallback = object : OnBackPressedCallback(true) { // callback is enabled by default
            override fun handleOnBackPressed() {
                // This code will be executed when the back button is pressed
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    // If WebView cannot go back, then let the system handle the back press
                    // (which will typically finish the activity or go to the previous activity)
                    this.isEnabled = false // Disable this callback to allow default behavior
                    onBackPressedDispatcher.onBackPressed() // Manually dispatch to let others handle
                }
            }
        }
        // Register the callback with the dispatcher
        // You can register it in onCreate or onResume
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        // If you need to enable/disable the callback dynamically, you can do:
        // onBackPressedCallback.isEnabled = false // To temporarily disable it
        // onBackPressedCallback.isEnabled = true  // To enable it again
    }

    // You can also remove the callback when the activity is destroyed to prevent leaks
    // Though addCallback(owner, callback) handles this automatically if 'owner' is lifecycle aware.
    @Override
    override fun onDestroy() {
        super.onDestroy()
        // onBackPressedCallback.remove() // Not strictly necessary if added with 'this' as owner
    }
}