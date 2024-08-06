package com.example.tnbcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.example.tnbcalculator.ui.theme.TNBCalculatorTheme

class MainActivity : ComponentActivity() {

    private lateinit var database: DatabaseReference
    private var previousReading by mutableStateOf(0.0)
    private var currentReading by mutableStateOf("")
    private var resultText by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TNBCalculatorTheme {
                Scaffold(
                    content = { padding ->
                        MainScreen(Modifier.padding(padding))
                    }
                )
            }
        }

        // Initialize Firebase
        database = FirebaseDatabase.getInstance().reference

        // Retrieve previous reading from Firebase
        getPreviousReading()
    }

    @Composable
    fun MainScreen(modifier: Modifier = Modifier) {
        val paddingValues = 16.dp

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = currentReading,
                onValueChange = { currentReading = it },
                label = { Text("Current Reading (kWh)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(paddingValues))

            Button(
                onClick = { calculateBill() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Calculate")
            }

            Spacer(modifier = Modifier.height(paddingValues))

            Text(text = resultText)
        }
    }

    private fun getPreviousReading() {
        database.child("previousReading").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    previousReading = dataSnapshot.getValue(Double::class.java) ?: 0.0
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun calculateBill() {
        if (currentReading.isEmpty()) {
            resultText = "Please enter the current reading."
            return
        }

        val currentReadingValue = currentReading.toDoubleOrNull() ?: return
        if (currentReadingValue < previousReading) {
            resultText = "Current reading must be higher than previous reading."
            return
        }

        val usage = currentReadingValue - previousReading
        val bill = calculateTNB(usage)

        resultText = String.format("Current month usage: %.2f kWh\nExpected Bill: RM %.2f", usage, bill)
    }

    private fun calculateTNB(usage: Double): Double {
        return when {
            usage <= 200 -> usage * 0.218
            usage <= 300 -> 200 * 0.218 + (usage - 200) * 0.334
            usage <= 600 -> 200 * 0.218 + 100 * 0.334 + (usage - 300) * 0.516
            else -> 200 * 0.218 + 100 * 0.334 + 300 * 0.516 + (usage - 600) * 0.546
        }
    }
}
