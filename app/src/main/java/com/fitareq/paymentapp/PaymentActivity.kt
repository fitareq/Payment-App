package com.fitareq.paymentapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.fitareq.paymentapp.databinding.ActivityPaymentBinding

class PaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val paymentMethod:String = intent.getStringExtra(Constants.KEY_PAYMENT_METHOD)?:""

    }
}