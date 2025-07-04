package com.example.uberremake.IntroScreens

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.uberremake.R
import com.example.uberremake.databinding.ActivityThirdIntroBinding
import com.example.uberremake.login.logInActivity

class ThirdIntro : AppCompatActivity() {
    private lateinit var binding: ActivityThirdIntroBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityThirdIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnThirdIntro.setOnClickListener {
            startActivity(Intent(this, logInActivity::class.java))
            finish()
        }

    }
}