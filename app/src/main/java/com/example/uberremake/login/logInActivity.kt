package com.example.uberremake.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.uberremake.DriverHomeActivity
import com.example.uberremake.MainActivity
import com.example.uberremake.R
import com.example.uberremake.databinding.ActivityLogInBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class logInActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLogInBinding

    private lateinit var auth: FirebaseAuth

    override fun onStart() {
        super.onStart()

        // Check if user already login in
        val currentUser: FirebaseUser? = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, DriverHomeActivity::class.java))
            finish()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLogInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = FirebaseAuth.getInstance()

        binding.signupBtn.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }

        binding.loginPhnBtn.setOnClickListener {
            startActivity(Intent(this, PhoneActivity::class.java))
            finish()
        }

        binding.loginBtn.setOnClickListener {
            val userName = binding.userName.text.toString()
            val password = binding.password.text.toString()

            if (userName.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill the details", Toast.LENGTH_SHORT).show()
            } else {
                auth.signInWithEmailAndPassword(userName, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Login Successfully", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, EditProfileActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "Login Failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }
    }
}