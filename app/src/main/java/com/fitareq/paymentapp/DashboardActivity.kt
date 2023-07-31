package com.fitareq.paymentapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.fitareq.paymentapp.Extensions.startActivity
import com.fitareq.paymentapp.databinding.ActivityDashboardBinding

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            bkash.setOnClickListener {
                startActivity(PaymentActivity::class.java){
                    this.putExtra(Constants.KEY_PAYMENT_METHOD, Constants.VALUE_BKASH)
                }
            }
            nagad.setOnClickListener {
                startActivity(PaymentActivity::class.java){
                    this.putExtra(Constants.KEY_PAYMENT_METHOD, Constants.VALUE_NAGAD)
                }
            }
        }

    }
}