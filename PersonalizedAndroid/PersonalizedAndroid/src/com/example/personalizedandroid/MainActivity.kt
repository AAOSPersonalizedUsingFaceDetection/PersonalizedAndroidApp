package com.example.personalizedandroid

import android.app.Activity
import android.app.ActivityManager
import android.app.IActivityManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.RemoteException
import android.os.UserManager
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import android.content.pm.UserInfo

class MainActivity : Activity() {

    private lateinit var userManager: UserManager
    private var activityManager: IActivityManager? = null

    private lateinit var userListTextView: TextView
    private lateinit var userNameInput: EditText
    private lateinit var userSpinner: Spinner
    private lateinit var themeSpinner: Spinner

    private lateinit var userAdapter: ArrayAdapter<String>
    private lateinit var themeAdapter: ArrayAdapter<CharSequence>

    private var isSpinnerInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mainLayout: LinearLayout = findViewById<LinearLayout>(R.id.main_layout)!!
userListTextView = findViewById<TextView>(R.id.user_list)!!
userNameInput = findViewById<EditText>(R.id.user_name_input)!!
userSpinner = findViewById<Spinner>(R.id.user_spinner)!!
themeSpinner = findViewById<Spinner>(R.id.theme_spinner)!!
val createUserButton: Button = findViewById<Button>(R.id.create_user_button)!!
val deleteUserButton: Button = findViewById<Button>(R.id.delete_user_button)!!


userManager = getSystemService(UserManager::class.java)!!

        activityManager = ActivityManager.getService()

        val intendedUserId = intent.getIntExtra(WelcomeActivity.EXTRA_USER_ID, -1)
        val currentUserId = getCurrentUserId()
        Log.d(TAG, "MainActivity: Intended user ID: $intendedUserId, Current user ID: $currentUserId")
        if (intendedUserId != -1 && intendedUserId != currentUserId && intendedUserId != 0) {
            switchUser(intendedUserId)
        }

applyTheme(userId, findViewById<LinearLayout>(R.id.main_layout)!!)

        applyPreferences()

        setupUserSpinner()
        setupThemeSpinner()
        listUsers()

        createUserButton.setOnClickListener {
            val name = userNameInput.text.toString().trim()
            if (!TextUtils.isEmpty(name)) {
                createNewUser(name)
                userNameInput.setText("")
                setupUserSpinner()
            } else {
                userListTextView.text = "Please enter a user name"
            }
        }

        deleteUserButton.setOnClickListener {
            val selectedPosition = userSpinner.selectedItemPosition
            if (selectedPosition != AdapterView.INVALID_POSITION) {
                val users = userManager.users
                if (selectedPosition < users.size) {
                    val selectedUser = users[selectedPosition]
                    deleteUser(selectedUser.id)
                    setupUserSpinner()
                }
            }
        }
    }

    private fun setupUserSpinner() {
        val users = userManager.users
        val userNames = ArrayList<String>()
        val currentUserId = getCurrentUserId()
        var currentUserPosition = 0

        for ((i, user) in users.withIndex()) {
            userNames.add("ID: ${user.id}, Name: ${user.name}")
            if (user.id == currentUserId) {
                currentUserPosition = i
            }
        }

        userAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, userNames)
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        userSpinner.adapter = userAdapter
        userSpinner.setSelection(currentUserPosition, false)
        isSpinnerInitialized = false

        userSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                    Log.d(TAG, "Spinner initialized, skipping switch for position: $position")
                    return
                }

                val selectedUser = users[position]
                Log.d(TAG, "Spinner selected user ID: ${selectedUser.id}, Name: ${selectedUser.name}")
                if (selectedUser.id != getCurrentUserId()) {
                    switchUser(selectedUser.id)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupThemeSpinner() {
        themeAdapter = ArrayAdapter.createFromResource(this, R.array.theme_options, android.R.layout.simple_spinner_item)
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        themeSpinner.adapter = themeAdapter

        val currentUserId = getCurrentUserId()
        val currentTheme = loadUserPreferences(currentUserId)

        val themePosition = when (currentTheme) {
            "solid_red" -> 1
            "solid_green" -> 2
            else -> 0
        }

        themeSpinner.setSelection(themePosition, false)

        themeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val theme = when (position) {
                    1 -> "solid_red"
                    2 -> "solid_green"
                    else -> "gradient_blue"
                }

                val userId = getCurrentUserId()
                saveUserPreferences(userId, theme)
                applyTheme(userId, findViewById<LinearLayout>(R.id.main_layout)!!)
                applyPreferences()
                Log.d(TAG, "Theme selected: $theme for user ID: $userId")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun listUsers() {
        val users = userManager.users
        val userText = StringBuilder("Users:\n")
        for (user in users) {
            userText.append("ID: ${user.id}, Name: ${user.name}, Theme: ${loadUserPreferences(user.id)}\n")
        }
        userListTextView.text = userText.toString()
    }

    private fun createNewUser(name: String) {
        try {
            val newUser = userManager.createUser(name, UserManager.USER_TYPE_FULL_SECONDARY, 0)
            if (newUser != null) {
                Log.d(TAG, "User created: ${newUser.name}")
                userListTextView.text = "User created: ${newUser.name}"
                saveUserPreferences(newUser.id, "gradient_blue")
                listUsers()
            } else {
                Log.e(TAG, "Failed to create user: null result")
                userListTextView.text = "Failed to create user: null result"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create user: ${e.message}")
            userListTextView.text = "Failed to create user: ${e.message}"
        }
    }

    private fun switchUser(userId: Int) {
        try {
            Log.d(TAG, "Attempting to switch to user ID: $userId")
            activityManager?.switchUser(userId)
            Log.d(TAG, "Switched to user ID: $userId")
            userListTextView.text = "Switched to user ID: $userId"
            applyTheme(userId, findViewById<LinearLayout>(R.id.main_layout)!!)
            applyPreferences()
            listUsers()
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to switch user: ${e.message}")
            userListTextView.text = "Failed to switch user: ${e.message}"
        }
    }

    private fun deleteUser(userId: Int) {
        if (userId == getCurrentUserId()) {
            userListTextView.text = "Cannot delete current user"
            return
        }
        if (userId == 0) {
            userListTextView.text = "Cannot delete system user"
            return
        }
        try {
            val success = userManager.removeUser(userId)
            if (success) {
                Log.d(TAG, "User deleted: ID $userId")
                userListTextView.text = "User deleted: ID $userId"
                listUsers()
            } else {
                Log.e(TAG, "Failed to delete user: ID $userId")
                userListTextView.text = "Failed to delete user: ID $userId"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete user: ${e.message}")
            userListTextView.text = "Failed to delete user: ${e.message}"
        }
    }

    private fun saveUserPreferences(userId: Int, theme: String) {
        val prefs = getSharedPreferences("user_$userId", MODE_PRIVATE)
        prefs.edit().putString("theme", theme).apply()
    }

    private fun loadUserPreferences(userId: Int): String {
        val prefs = getSharedPreferences("user_$userId", MODE_PRIVATE)
        return prefs.getString("theme", "gradient_blue") ?: "gradient_blue"
    }

    private fun applyTheme(userId: Int, layout: LinearLayout) {
        val backgroundRes = when (loadUserPreferences(userId)) {
            "solid_red" -> R.drawable.solid_red
            "solid_green" -> R.drawable.solid_green
            else -> R.drawable.gradient_blue
        }
        layout.setBackgroundResource(backgroundRes)
    }

    private fun applyPreferences() {
        val userId = getCurrentUserId()
        val theme = loadUserPreferences(userId)
        if (theme == "dark") {
            userListTextView.setBackgroundColor(getColor(android.R.color.black))
            userListTextView.setTextColor(getColor(android.R.color.white))
        } else {
            userListTextView.setBackgroundColor(getColor(android.R.color.white))
            userListTextView.setTextColor(getColor(android.R.color.black))
        }
    }

    private fun getCurrentUserId(): Int {
        return userManager.getUserInfo(android.os.Process.myUserHandle().identifier)?.id?.also {
            Log.d(TAG, "getCurrentUserId: ID: $it")
        } ?: run {
            Log.e(TAG, "getCurrentUserId: UserInfo is null")
            -1
        }
    }

    companion object {
        private const val TAG = "PersonalizedAndroid"
    }
}

