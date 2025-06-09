package com.example.uberremake

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.uberremake.databinding.ActivityMainBinding
import com.example.uberremake.login.logInActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn.getClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.auth

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        // Initialize Firebase (optional, but safe)
//        FirebaseApp.initializeApp(this)
//
//        // Install App Check provider before any Firebase usage
//        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
//            PlayIntegrityAppCheckProviderFactory.getInstance()
//        )
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.logoutBtn.setOnClickListener{
            Firebase.auth.signOut()
            finish()

            val intent = Intent(this, logInActivity::class.java)
            startActivity(intent)
            Toast.makeText(this, "Logout Successfully", Toast.LENGTH_SHORT).show()
        }
    }
}