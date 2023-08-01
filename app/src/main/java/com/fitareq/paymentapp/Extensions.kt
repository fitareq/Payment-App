package com.fitareq.paymentapp

import android.app.Activity
import android.content.Intent
import android.widget.EditText
import android.widget.Toast

object Extensions {
    val EditText.value get() = this.text?.toString()
    fun Activity.startActivity(cls: Class<*>, finishThis: Boolean = false, block:(Intent.()->Unit)? = null){
        val intent = Intent(this, cls)
        block?.invoke(intent)
        startActivity(intent)
        if (finishThis) finish()
    }

    fun Activity.toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}