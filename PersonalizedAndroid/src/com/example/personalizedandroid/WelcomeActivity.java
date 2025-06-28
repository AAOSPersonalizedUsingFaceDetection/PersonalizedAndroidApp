package com.example.personalizedandroid;

import android.content.Intent;
import android.os.Bundle;
import android.os.UserManager;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.app.Activity;
import android.content.pm.UserInfo;

public class WelcomeActivity extends Activity {
    private static final String TAG = "PersonalizedAndroid";
    private UserManager mUserManager;
    public static final String EXTRA_USER_ID = "extra_user_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        TextView welcomeText = findViewById(R.id.welcome_text);
        Button continueButton = findViewById(R.id.continue_button);

        // Initialize UserManager
        mUserManager = getSystemService(UserManager.class);

        // Set welcome message with current user's name
        UserInfo currentUser = getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "WelcomeActivity: Current user ID: " + currentUser.id + ", Name: " + currentUser.name);
            welcomeText.setText("Welcome, " + currentUser.name + "!");
        } else {
            Log.e(TAG, "WelcomeActivity: No current user found");
            welcomeText.setText("Welcome!");
        }

        // Navigate to MainActivity on button click
        continueButton.setOnClickListener(v -> {
            Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
            if (currentUser != null) {
                intent.putExtra(EXTRA_USER_ID, currentUser.id);
            }
            startActivity(intent);
            finish();
        });
    }

    private UserInfo getCurrentUser() {
        if (mUserManager != null) {
            UserInfo userInfo = mUserManager.getUserInfo(android.os.Process.myUserHandle().getIdentifier());
            if (userInfo != null) {
                Log.d(TAG, "getCurrentUser: ID: " + userInfo.id + ", Name: " + userInfo.name);
            } else {
                Log.e(TAG, "getCurrentUser: UserInfo is null");
            }
            return userInfo;
        }
        Log.e(TAG, "getCurrentUser: UserManager is null");
        return null;
    }
}