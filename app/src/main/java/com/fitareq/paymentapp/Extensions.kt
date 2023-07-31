package com.fitareq.paymentapp

import android.app.Activity
import android.content.Intent
import android.widget.EditText

object Extensions {
    val EditText.value get() = this.text?.toString()?:""
    fun Activity.startActivity(cls: Class<*>, finishThis: Boolean = false, block:(Intent.()->Unit)? = null){
        val intent = Intent(this, cls)
        block?.invoke(intent)
        startActivity(intent)
        if (finishThis) finish()
    }
}