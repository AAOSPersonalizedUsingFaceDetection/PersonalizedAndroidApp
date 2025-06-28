package com.example.personalizedandroid;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.pm.UserInfo;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "PersonalizedAndroid";
    private UserManager mUserManager;
    private IActivityManager mActivityManager;
    private TextView mUserListTextView;
    private EditText mUserNameInput;
    private Spinner mUserSpinner;
    private ArrayAdapter<String> mUserAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUserListTextView = findViewById(R.id.user_list);
        mUserNameInput = findViewById(R.id.user_name_input);
        mUserSpinner = findViewById(R.id.user_spinner);
        Button createUserButton = findViewById(R.id.create_user_button);
        Button deleteUserButton = findViewById(R.id.delete_user_button);

        // Initialize UserManager
        mUserManager = getSystemService(UserManager.class);

        // Initialize ActivityManager
        mActivityManager = ActivityManager.getService();

        // Check and restore intended user
        int intendedUserId = getIntent().getIntExtra(WelcomeActivity.EXTRA_USER_ID, -1);
        int currentUserId = getCurrentUserId();
        Log.d(TAG, "MainActivity: Intended user ID: " + intendedUserId + ", Current user ID: " + currentUserId);
        if (intendedUserId != -1 && intendedUserId != currentUserId && intendedUserId != 0) {
            switchUser(intendedUserId);
        }

        // Apply current user's preferences
        applyPreferences();

        // Set up user spinner
        setupUserSpinner();

        // List existing users
        listUsers();

        // Set up button to create a new user
        createUserButton.setOnClickListener(v -> {
            String name = mUserNameInput.getText().toString().trim();
            if (!TextUtils.isEmpty(name)) {
                createNewUser(name);
                mUserNameInput.setText(""); // Clear input after creation
                setupUserSpinner(); // Refresh spinner
            } else {
                mUserListTextView.setText("Please enter a user name");
            }
        });

        // Set up button to delete selected user
        deleteUserButton.setOnClickListener(v -> {
            int selectedPosition = mUserSpinner.getSelectedItemPosition();
            if (selectedPosition != AdapterView.INVALID_POSITION) {
                List<UserInfo> users = mUserManager.getUsers();
                if (selectedPosition < users.size()) {
                    UserInfo selectedUser = users.get(selectedPosition);
                    deleteUser(selectedUser.id);
                    setupUserSpinner(); // Refresh spinner
                }
            }
        });
    }

    private void setupUserSpinner() {
        if (mUserManager != null) {
            List<UserInfo> users = mUserManager.getUsers();
            List<String> userNames = new ArrayList<>();
            for (UserInfo user : users) {
                userNames.add("ID: " + user.id + ", Name: " + user.name);
            }
            mUserAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, userNames);
            mUserAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mUserSpinner.setAdapter(mUserAdapter);

            // Switch user on selection
            mUserSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    List<UserInfo> users = mUserManager.getUsers();
                    if (position < users.size()) {
                        UserInfo selectedUser = users.get(position);
                        if (selectedUser.id != getCurrentUserId()) {
                            switchUser(selectedUser.id);
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });
        }
    }

    private void listUsers() {
        if (mUserManager != null) {
            List<UserInfo> users = mUserManager.getUsers();
            StringBuilder userText = new StringBuilder("Users:\n");
            for (UserInfo user : users) {
                userText.append("ID: ").append(user.id)
                        .append(", Name: ").append(user.name)
                        .append(", Theme: ").append(loadUserPreferences(user.id)).append("\n");
            }
            mUserListTextView.setText(userText.toString());
        } else {
            mUserListTextView.setText("UserManager not initialized");
        }
    }

    private void createNewUser(String name) {
        if (mUserManager != null) {
            try {
                UserInfo newUser = mUserManager.createUser(name, UserManager.USER_TYPE_FULL_SECONDARY, 0);
                if (newUser != null) {
                    Log.d(TAG, "User created: " + newUser.name);
                    mUserListTextView.setText("User created: " + newUser.name);
                    saveUserPreferences(newUser.id, "dark"); // Default theme
                    listUsers();
                } else {
                    Log.e(TAG, "Failed to create user: null result");
                    mUserListTextView.setText("Failed to create user: null result");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to create user: " + e.getMessage());
                mUserListTextView.setText("Failed to create user: " + e.getMessage());
            }
        } else {
            mUserListTextView.setText("UserManager not initialized");
        }
    }

    private void switchUser(int userId) {
        if (mActivityManager != null) {
            try {
                mActivityManager.switchUser(userId);
                Log.d(TAG, "Switched to user ID: " + userId);
                mUserListTextView.setText("Switched to user ID: " + userId);
                applyPreferences();
                listUsers();
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to switch user: " + e.getMessage());
                mUserListTextView.setText("Failed to switch user: " + e.getMessage());
            }
        } else {
            mUserListTextView.setText("ActivityManager not initialized");
        }
    }

    private void deleteUser(int userId) {
        if (mUserManager != null) {
            if (userId == getCurrentUserId()) {
                mUserListTextView.setText("Cannot delete current user");
                return;
            }
            if (userId == 0) {
                mUserListTextView.setText("Cannot delete system user");
                return;
            }
            try {
                boolean success = mUserManager.removeUser(userId);
                if (success) {
                    Log.d(TAG, "User deleted: ID " + userId);
                    mUserListTextView.setText("User deleted: ID " + userId);
                    listUsers();
                } else {
                    Log.e(TAG, "Failed to delete user: ID " + userId);
                    mUserListTextView.setText("Failed to delete user: ID " + userId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete user: " + e.getMessage());
                mUserListTextView.setText("Failed to delete user: " + e.getMessage());
            }
        } else {
            mUserListTextView.setText("UserManager not initialized");
        }
    }

    private void saveUserPreferences(int userId, String theme) {
        SharedPreferences prefs = getSharedPreferences("user_" + userId, MODE_PRIVATE);
        prefs.edit().putString("theme", theme).apply();
    }

    private String loadUserPreferences(int userId) {
        SharedPreferences prefs = getSharedPreferences("user_" + userId, MODE_PRIVATE);
        return prefs.getString("theme", "default");
    }

    private void applyPreferences() {
        int userId = getCurrentUserId();
        String theme = loadUserPreferences(userId);
        if ("dark".equals(theme)) {
            mUserListTextView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mUserListTextView.setTextColor(getResources().getColor(android.R.color.white));
        } else {
            mUserListTextView.setBackgroundColor(getResources().getColor(android.R.color.white));
            mUserListTextView.setTextColor(getResources().getColor(android.R.color.black));
        }
    }

    private int getCurrentUserId() {
        if (mUserManager != null) {
            UserInfo currentUser = mUserManager.getUserInfo(android.os.Process.myUserHandle().getIdentifier());
            if (currentUser != null) {
                Log.d(TAG, "getCurrentUserId: ID: " + currentUser.id + ", Name: " + currentUser.name);
                return currentUser.id;
            } else {
                Log.e(TAG, "getCurrentUserId: UserInfo is null");
            }
        } else {
            Log.e(TAG, "getCurrentUserId: UserManager is null");
        }
        return -1;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}