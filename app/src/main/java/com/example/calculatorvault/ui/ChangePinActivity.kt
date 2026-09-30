package com.example.calculatorvault.ui

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.calculatorvault.R
import com.example.calculatorvault.databinding.ActivityChangePinBinding
import com.example.calculatorvault.security.PinManager
import com.example.calculatorvault.util.VaultSession

class ChangePinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePinBinding
    private lateinit var pinManager: PinManager

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        super.onCreate(savedInstanceState)
        binding = ActivityChangePinBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        pinManager = PinManager(this)

        binding.btnSavePin.setOnClickListener { onSaveClicked() }
    }

    override fun onResume() {
        super.onResume()
        if (!VaultSession.isUnlocked()) {
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun onSaveClicked() {
        val current = binding.etCurrentPin.text.toString()
        val newPin = binding.etNewPin.text.toString()
        val confirm = binding.etConfirmPin.text.toString()

        when (val result = pinManager.verifyPin(current)) {
            is PinManager.VerifyResult.LockedOut -> {
                val seconds = (result.remainingMillis / 1000).coerceAtLeast(1)
                Toast.makeText(this, "${getString(R.string.lockout_generic_toast)} (${seconds}s)", Toast.LENGTH_LONG).show()
                return
            }
            is PinManager.VerifyResult.Incorrect -> {
                Toast.makeText(this, "PIN saat ini salah.", Toast.LENGTH_SHORT).show()
                return
            }
            is PinManager.VerifyResult.Correct -> { /* proceed */ }
        }

        if (!pinManager.isPinFormatValid(newPin)) {
            Toast.makeText(this, R.string.error_pin_length, Toast.LENGTH_SHORT).show()
            return
        }
        if (pinManager.isPinObviouslyWeak(newPin)) {
            Toast.makeText(this, R.string.error_pin_weak, Toast.LENGTH_SHORT).show()
            return
        }
        if (newPin != confirm) {
            Toast.makeText(this, R.string.error_pin_mismatch, Toast.LENGTH_SHORT).show()
            return
        }

        pinManager.setPin(newPin)
        Toast.makeText(this, "PIN berhasil diganti.", Toast.LENGTH_SHORT).show()
        finish()
    }
}
