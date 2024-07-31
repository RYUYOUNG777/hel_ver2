package com.example.hel_ver2

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val settingsButton: Button = findViewById(R.id.settings_button)
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        val anotherSettingsButton: Button = findViewById(R.id.another_settings_button)
        anotherSettingsButton.setOnClickListener {
            val intent = Intent(this, AnotherSettingActivity::class.java)
            startActivity(intent)
        }
    }
}
