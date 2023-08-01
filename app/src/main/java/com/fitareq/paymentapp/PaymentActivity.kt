package com.fitareq.paymentapp


import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.fitareq.paymentapp.databinding.ActivityPaymentBinding
import com.fitareq.paymentapp.databinding.ConfirmDialogBinding
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class PaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val paymentMethod: String = intent.getStringExtra(Constants.KEY_PAYMENT_METHOD) ?: ""

        binding.apply {
            numberLay.hint = getString(R.string.number_hint, paymentMethod)
            nameLay.hint = getString(R.string.name_hint, paymentMethod)

            submit.setOnClickListener {
                showConfirmDialog()
            }
        }

    }

    private fun showConfirmDialog() {
        val dialog = Dialog(this)
        val dBinding = ConfirmDialogBinding.inflate(layoutInflater)
        dialog.setContentView(dBinding.root)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        dBinding.download.setOnClickListener {
            createPdfFromDialog(this, dBinding.mainLayout)
        }

        dialog.show()
    }

    private fun createPdfFromDialog(context: Context, view: View) {
        val bitmap: Bitmap = createBitmapFromDialog(view)
        generatePDF(context, bitmap)
    }

    private fun createBitmapFromDialog(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun generatePDF(context: Context, bitmap: Bitmap) {
        val document = PdfDocument()
        val pageInfo = PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Draw the Bitmap on the PDF canvas
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        document.finishPage(page)

        // Save the PDF to a file
        val fileName = "dialog_content.pdf"

        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)

        val contentResolver = context.contentResolver
        val contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val pdfUri = contentResolver.insert(contentUri, contentValues)

        try {
            val outputStream = contentResolver.openOutputStream(pdfUri!!)
            document.writeTo(outputStream)
            outputStream!!.flush()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        document.close()
    }

    private fun getCurrentDateTime(): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return simpleDateFormat.format(Calendar.getInstance().time)
    }
}