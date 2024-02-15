package `in`.app.connect.utils


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View
import com.google.android.material.snackbar.Snackbar

class NetworkUtils {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var CONTEXT: Context

        fun isNetworkAvailable(context: Context): Boolean {
            CONTEXT = context
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkCapabilities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                cm.getNetworkCapabilities(cm.activeNetwork)
            } else {
                null
            }
            return networkCapabilities?.run {
                hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } ?: false
        }

        private fun showSnackBar(
            view: View
        ) {
            val snackBar = Snackbar.make(view, "No internet connectivity", Snackbar.LENGTH_INDEFINITE)
            snackBar.setAction("Retry") {
                if (isNetworkAvailable(CONTEXT)) {
                    // Network is available
                    snackBar.dismiss()
                } else {
                    // Network is still not available, show another snackBar
                    showNoInternetSnackBar(view)
                }
            }
            snackBar.addCallback(object: Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    if (event == Snackbar.Callback.DISMISS_EVENT_ACTION) {
                        // Retry button was pressed, recreate the activity
                        (view.context as Activity).recreate()
                    }
                }
            })
            snackBar.show()
        }

        fun showNoInternetSnackBar(view: View) {
            showSnackBar(view)
        }
    }
}