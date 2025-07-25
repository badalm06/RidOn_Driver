package com.example.uberremake

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.uberremake.IntroScreens.FirstIntro
import com.example.uberremake.login.User
import com.example.uberremake.login.logInActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging

class SplashScreen : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash_screen)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {

            // ✅ User is logged in, load user profile
            FirebaseDatabase.getInstance().getReference("users")
                .child(user.uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val name = snapshot.child("name").getValue(String::class.java) ?: ""
                        val phone = snapshot.child("phone").getValue(String::class.java) ?: ""
                        val email = snapshot.child("email").getValue(String::class.java) ?: ""
                        val profileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java) ?: ""

                        // ✅ Set data to Common.currentUser
                        Common.currentUser = User().apply {
                            this.name = name
                            this.number = phone
                            this.email = email
                            this.profileImageUrl = profileImageUrl
                        }
                                // ✅ Go to home screen
                        startActivity(Intent(this@SplashScreen, DriverHomeActivity::class.java))
                        finish()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@SplashScreen, "Failed to load user data", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@SplashScreen, logInActivity::class.java))
                        finish()
                    }
                })
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, logInActivity::class.java))
                finish()
            }, 2000)
        }
    }

}