package com.example.calculatorvault

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.calculatorvault.databinding.ActivityMainBinding
import com.example.calculatorvault.security.PinManager
import com.example.calculatorvault.util.VaultSession
import com.example.calculatorvault.vault.VaultActivity

/**
 * Looks and behaves like a normal calculator at all times.
 *
 * The ONLY hidden behaviour: if the user types a plain number (no +,-,×,÷,%
 * used since the last AC) and presses "=", and that number matches the
 * secret PIN, the vault opens instead of just showing the number as a
 * result. Any wrong guess, or any calculation that used an operator, behaves
 * exactly like a stock calculator - nothing is shown, logged, or hinted.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var engine: CalculatorEngine
    private lateinit var pinManager: PinManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        engine = CalculatorEngine()
        pinManager = PinManager(this)

        setupButtonListeners()
        refreshDisplay()

        if (!pinManager.isPinSet()) {
            showFirstRunPinSetup()
        }
    }

    override fun onResume() {
        super.onResume()
        // If the vault was open and the app got backgrounded, VaultSession is
        // now locked; nothing else to do here since VaultActivity itself
        // finishes on resume-while-locked. Calculator screen needs no reset.
    }

    private fun setupButtonListeners() {
        val digitButtons = mapOf(
            binding.btn0 to '0', binding.btn1 to '1', binding.btn2 to '2',
            binding.btn3 to '3', binding.btn4 to '4', binding.btn5 to '5',
            binding.btn6 to '6', binding.btn7 to '7', binding.btn8 to '8',
            binding.btn9 to '9'
        )
        digitButtons.forEach { (button, digit) ->
            button.setOnClickListener {
                engine.inputDigit(digit)
                refreshDisplay()
            }
        }

        binding.btnDot.setOnClickListener { engine.inputDot(); refreshDisplay() }
        binding.btnPlusMinus.setOnClickListener { engine.inputPlusMinus(); refreshDisplay() }
        binding.btnPercent.setOnClickListener { engine.inputPercent(); refreshDisplay() }
        binding.btnBackspace.setOnClickListener { engine.backspace(); refreshDisplay() }
        binding.btnClear.setOnClickListener { engine.clear(); refreshDisplay() }

        binding.btnPlus.setOnClickListener { engine.inputOperator('+'); refreshDisplay() }
        binding.btnMinus.setOnClickListener { engine.inputOperator('-'); refreshDisplay() }
        binding.btnMultiply.setOnClickListener { engine.inputOperator('×'); refreshDisplay() }
        binding.btnDivide.setOnClickListener { engine.inputOperator('÷'); refreshDisplay() }

        binding.btnEquals.setOnClickListener { onEqualsPressed() }
    }

    private fun onEqualsPressed() {
        val rawInput = engine.currentText()
        val isCandidatePin = engine.wasPureNumberEntry() &&
            rawInput.matches(Regex("^\\d{4,8}\$"))

        // Always compute the normal calculator result first/regardless, so
        // the screen behaves identically whether or not this was a PIN try.
        val result = engine.equals()
        refreshDisplay()

        if (isCandidatePin && pinManager.isPinSet()) {
            when (pinManager.verifyPin(rawInput)) {
                is PinManager.VerifyResult.Correct -> {
                    VaultSession.unlock()
                    startActivity(Intent(this, VaultActivity::class.java))
                }
                is PinManager.VerifyResult.Incorrect -> {
                    // Silently do nothing extra - looks like a normal calculation.
                }
                is PinManager.VerifyResult.LockedOut -> {
                    // Stay silent here too: showing a toast would out the app
                    // as "not a real calculator" to anyone watching over the
                    // user's shoulder. The lockout still applies internally.
                }
            }
        }
    }

    private fun refreshDisplay() {
        binding.tvResult.text = engine.currentText()
        binding.tvExpression.text = engine.expressionPreview()
    }

    /**
     * First launch only: there is no PIN yet, so there is nothing to hide
     * behind a secret gesture yet. We ask the user to create one directly.
     */
    private fun showFirstRunPinSetup() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        val message = TextView(this).apply { text = getString(R.string.setup_message) }
        val pinInput = EditText(this).apply {
            hint = getString(R.string.hint_new_pin)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        val confirmInput = EditText(this).apply {
            hint = getString(R.string.hint_confirm_pin)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        container.addView(message)
        container.addView(pinInput)
        container.addView(confirmInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.setup_title)
            .setView(container)
            .setCancelable(false)
            .setPositiveButton(R.string.btn_save, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val pin = pinInput.text.toString()
                val confirm = confirmInput.text.toString()
                when {
                    !pinManager.isPinFormatValid(pin) ->
                        Toast.makeText(this, R.string.error_pin_length, Toast.LENGTH_SHORT).show()
                    pinManager.isPinObviouslyWeak(pin) ->
                        Toast.makeText(this, R.string.error_pin_weak, Toast.LENGTH_SHORT).show()
                    pin != confirm ->
                        Toast.makeText(this, R.string.error_pin_mismatch, Toast.LENGTH_SHORT).show()
                    else -> {
                        pinManager.setPin(pin)
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }
}
