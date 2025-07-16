package com.example.uberremake

import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class EditBankDetailsActivity : AppCompatActivity() {

    private lateinit var etAccountHolder: EditText
    private lateinit var etAccountNumber: EditText
    private lateinit var etIfsc: EditText
    private lateinit var etPan: EditText
    private lateinit var btnSaveBankDetails: Button
    private lateinit var spinnerBankName: Spinner

    // Reference to your database and user
    private val user = FirebaseAuth.getInstance().currentUser
    private val database = FirebaseDatabase.getInstance().getReference("users")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_bank_details)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        etAccountHolder = findViewById(R.id.etAccountHolder)
        etAccountNumber = findViewById(R.id.etAccountNumber)
        etIfsc = findViewById(R.id.etIfsc)
        etPan = findViewById(R.id.etPan)
        btnSaveBankDetails = findViewById(R.id.btnSaveBankDetails)
        spinnerBankName = findViewById(R.id.spinnerBankName)

        // Setup Spinner with bank names
        val bankNames = resources.getStringArray(R.array.bank_names)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, bankNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBankName.adapter = adapter

        // Load existing bank details if any
        loadBankDetails()

        btnSaveBankDetails.setOnClickListener {
            saveBankDetails()
        }
    }

    private fun loadBankDetails() {
        user?.let { currentUser ->
            database.child(currentUser.uid).child("bankDetails")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        etAccountHolder.setText(snapshot.child("accountHolder").getValue(String::class.java) ?: "")
                        etAccountNumber.setText(snapshot.child("accountNumber").getValue(String::class.java) ?: "")
                        etIfsc.setText(snapshot.child("ifsc").getValue(String::class.java) ?: "")
                        etPan.setText(snapshot.child("pan").getValue(String::class.java) ?: "")

                        // Set Spinner to saved bank name if available
                        val savedBankName = snapshot.child("bankName").getValue(String::class.java) ?: ""
                        val bankNames = resources.getStringArray(R.array.bank_names)
                        val bankPosition = bankNames.indexOf(savedBankName)
                        if (bankPosition >= 0) {
                            spinnerBankName.setSelection(bankPosition)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    private fun saveBankDetails() {
        val accountHolder = etAccountHolder.text.toString().trim()
        val accountNumber = etAccountNumber.text.toString().trim()
        val ifsc = etIfsc.text.toString().trim()
        val bankName = spinnerBankName.selectedItem.toString()
        val pan = etPan.text.toString().trim()

        if (accountHolder.isEmpty() || accountNumber.isEmpty() || ifsc.isEmpty() || bankName.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val bankDetails = mapOf(
            "accountHolder" to accountHolder,
            "accountNumber" to accountNumber,
            "ifsc" to ifsc,
            "bankName" to bankName,
            "pan" to pan
        )

        user?.let { currentUser ->
            database.child(currentUser.uid).child("bankDetails").setValue(bankDetails)
                .addOnSuccessListener {
                    Toast.makeText(this, "Bank details saved", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save bank details", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // Handle toolbar back button
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
