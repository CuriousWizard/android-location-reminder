package com.curiouswizard.locationreminder.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.curiouswizard.locationreminder.R
import com.curiouswizard.locationreminder.databinding.ActivityAuthenticationBinding
import com.curiouswizard.locationreminder.locationreminders.RemindersActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AuthenticationActivity"
    }

    // View binding
    private lateinit var binding: ActivityAuthenticationBinding
    private val viewModel by viewModels<AuthenticationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            launchSignInFlow()
        }

        viewModel.authenticationState.observe(this, {authenticationState ->
            when (authenticationState) {
                AuthenticationViewModel.AuthenticationState.AUTHENTICATED -> {
                    // When the user is authenticated they should be navigated to the Reminders screen.
                    val remindersIntent = Intent(applicationContext, RemindersActivity::class.java)
                    startActivity(remindersIntent)
                    finish()
                }
                else -> Log.d(TAG, "User not authenticated")
            }
        })

    }

    // Creating an ActivityResultLauncher for our authentication intent and define what happens with the result
    private val startAuthForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val response = IdpResponse.fromResultIntent(result.data)
        if (result.resultCode == Activity.RESULT_OK) {
            // Successfully signed in user.
            Log.i(
                TAG,
                "Successfully signed in user " +
                        "${FirebaseAuth.getInstance().currentUser?.displayName}!"
            )

        } else {
            // Sign in failed. If response is null the user canceled the sign-in flow using
            // the back button. Otherwise check response.getError().getErrorCode() and handle
            // the error.
            val message = "Sign in unsuccessful ${response?.error?.errorCode}"
            Log.i(TAG, message)
            Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchSignInFlow() {
        // Give users the option to sign in / register with their email or Google account. If users
        // choose to register with their email, they will need to create a password as well.
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch sign-in intent in the recommended way.
        startAuthForResult.launch(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setLogo(R.drawable.map)
                .build()
        )
    }
}
