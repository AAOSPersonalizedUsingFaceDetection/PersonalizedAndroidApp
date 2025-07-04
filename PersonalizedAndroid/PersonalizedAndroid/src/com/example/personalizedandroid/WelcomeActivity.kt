package com.example.personalizedandroid

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.UserManager
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.content.pm.UserInfo

class WelcomeActivity : Activity() {
    private lateinit var userManager: UserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val welcomeLayout: LinearLayout = findViewById<LinearLayout>(R.id.welcome_layout)!!
val welcomeText: TextView = findViewById<TextView>(R.id.welcome_text)!!
val continueButton: Button = findViewById<Button>(R.id.continue_button)!!


userManager = getSystemService(UserManager::class.java)!!

        val currentUser = getCurrentUser()
        if (currentUser != null) {
            Log.d(TAG, "WelcomeActivity: Current user ID: ${currentUser.id}, Name: ${currentUser.name}")
            welcomeText.text = "Welcome, ${currentUser.name}!"
            applyTheme(currentUser.id, welcomeLayout)
        } else {
            Log.e(TAG, "WelcomeActivity: No current user found")
            welcomeText.text = "Welcome!"
            welcomeLayout.setBackgroundResource(R.drawable.gradient_blue)
        }

        continueButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            currentUser?.let {
                intent.putExtra(EXTRA_USER_ID, it.id)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun getCurrentUser(): UserInfo? {
        return userManager.getUserInfo(android.os.Process.myUserHandle().identifier)?.also {
            Log.d(TAG, "getCurrentUser: ID: ${it.id}, Name: ${it.name}")
        } ?: run {
            Log.e(TAG, "getCurrentUser: UserInfo is null")
            null
        }
    }

    private fun applyTheme(userId: Int, layout: LinearLayout) {
        when (loadUserPreferences(userId)) {
            "solid_red" -> layout.setBackgroundResource(R.drawable.solid_red)
            "solid_green" -> layout.setBackgroundResource(R.drawable.solid_green)
            else -> layout.setBackgroundResource(R.drawable.gradient_blue)
        }
    }

    private fun loadUserPreferences(userId: Int): String {
        val prefs: SharedPreferences = getSharedPreferences("user_$userId", MODE_PRIVATE)
        return prefs.getString("theme", "gradient_blue") ?: "gradient_blue"
    }

    companion object {
        private const val TAG = "PersonalizedAndroid"
        const val EXTRA_USER_ID = "extra_user_id"
    }
}

