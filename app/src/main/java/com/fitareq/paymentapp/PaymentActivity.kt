package com.fitareq.paymentapp


import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fitareq.paymentapp.Extensions.toast
import com.fitareq.paymentapp.Extensions.value
import com.fitareq.paymentapp.databinding.ActivityPaymentBinding
import com.fitareq.paymentapp.databinding.ConfirmDialogBinding
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class PaymentActivity : AppCompatActivity() {
    private val REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 1
    private val REQUEST_LOCATION_PERMISSION = 2

    private lateinit var binding: ActivityPaymentBinding
    private lateinit var paymentMethod: String
    private lateinit var bitmap: Bitmap

    private var address: String = "n/a"
    private var uri: Uri? = null
    private var dialog: Dialog? = null
    private var currentDate="n/a"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getUserCurrentLocation()
        currentDate = getCurrentDateTime()

        paymentMethod = intent.getStringExtra(Constants.KEY_PAYMENT_METHOD) ?: ""

        binding.apply {
            numberLay.hint = getString(R.string.number_hint, paymentMethod)
            nameLay.hint = getString(R.string.name_hint, paymentMethod)

            submit.setOnClickListener {
                showConfirmDialog()
            }
        }

    }

    private fun showConfirmDialog() {
        if (dialog == null)
            dialog = Dialog(this)
        val dBinding = ConfirmDialogBinding.inflate(layoutInflater)
        dialog?.setContentView(dBinding.root)
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        dBinding.apply {
            numberValue.text = binding.number.value
            nameValue.text = binding.name.value
            amountValue.text = binding.amount.value
            narrationValue.text = binding.narration.value

            date.text = currentDate
            location.text = address

            if (paymentMethod == Constants.VALUE_NAGAD) {
                methodLogo.setImageResource(R.drawable.ic_nagad)
                methodLogoSub.text = getString(R.string.method_logo_sub, paymentMethod)
                numberLabel.text = getString(R.string.number_hint, paymentMethod)
                nameLabel.text = getString(R.string.name_hint, paymentMethod)
            }
            download.setOnClickListener {
                createPdfFromDialog(dBinding.mainLayout)
            }
            cancelButton.setOnClickListener {
                dialog?.dismiss()
            }
            share.setOnClickListener {
                if (uri != null) {
                    showShareOption()
                }
            }
        }


        dialog?.show()
    }

    private fun showShareOption() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, "Share PDF via..."))
    }

    private fun createPdfFromDialog(view: View) {
        bitmap = createBitmapFromDialog(view)
        checkWriteExternalStoragePermission()
    }
    private fun checkWriteExternalStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                generatePDF(this, bitmap)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION
                )
            }
        } else {
            generatePDF(this, bitmap)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generatePDF(this, bitmap)
            } else {
                toast("External storage access permission denied. PDF generation is not possible.")
            }
        }else if (requestCode == REQUEST_LOCATION_PERMISSION){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED ){
                findLocation()
            }else{
                toast("Access location permission denied.")
            }
        }
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
        val fileName = "invoice_${System.currentTimeMillis()}.pdf"

        var pdfFile: File? = null
        var pdfUri: Uri? = null
        if (isAndroidVersionOlderThan10()) {
            pdfFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                fileName
            )
        } else {
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            contentValues.put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DOCUMENTS
            )

            val contentResolver = context.contentResolver
            val contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            pdfUri = contentResolver.insert(contentUri, contentValues)
        }
        try {
            val outputStream =
                if (isAndroidVersionOlderThan10()) FileOutputStream(pdfFile!!)
                else contentResolver.openOutputStream(
                    pdfUri!!
                )
            document.writeTo(outputStream)
            outputStream!!.flush()
            outputStream.close()
            uri = if (isAndroidVersionOlderThan10()) Uri.fromFile(pdfFile!!) else pdfUri
            //toast("Successfully downloaded...")
            showSnackbarWithOpenOption()
        } catch (e: IOException) {
            e.printStackTrace()
            toast("Download failed!")
        }
        document.close()
    }

    private fun isAndroidVersionOlderThan10(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    private fun showSnackbarWithOpenOption() {
        Snackbar.make(
            dialog!!.findViewById(android.R.id.content),
            "PDF generated. Open the file?",
            Snackbar.LENGTH_LONG
        ).setAction("Open") {
            openPDFWithDefaultApp(uri!!)
        }.show()
    }

    private fun openPDFWithDefaultApp(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/pdf")
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Handle the case where no PDF reader app is installed on the device
            toast("No PDF reader app found.")
        }
    }

    private fun getCurrentDateTime(): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return simpleDateFormat.format(Calendar.getInstance().time)
    }

    private fun getUserCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ){
            findLocation()
        }else{
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), REQUEST_LOCATION_PERMISSION
            )
        }
    }
    @SuppressLint("MissingPermission")
    private fun findLocation() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val geocoder =
                    Geocoder(this, Locale.getDefault())
                try {
                    val addressList =
                        geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    address = addressList?.get(0)?.getAddressLine(0) ?: "n/a"
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

}