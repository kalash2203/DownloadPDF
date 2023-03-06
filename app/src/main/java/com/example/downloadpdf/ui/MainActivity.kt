package com.example.downloadpdf

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.downloadpdf.databinding.ActivityMainBinding
import com.example.downloadpdf.util.Constants
import com.example.downloadpdf.util.Constants.WRITE_PDF_REQUEST_CODE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL


class MainActivity : AppCompatActivity() {
    var binding : ActivityMainBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        binding?.txtDownloadPdf?.setOnClickListener {
            binding?.downloadingIcon?.visibility= View.VISIBLE
            checkPermission()
        }


    }


    private fun checkPermission(){
        val permission = WRITE_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            // If Permission is not granted, we will request it
            ActivityCompat.requestPermissions(this, arrayOf(permission), WRITE_PDF_REQUEST_CODE)
        } else {
            downloadPDF()
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            WRITE_PDF_REQUEST_CODE -> {
                // Check if the permission was granted or denied
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                         downloadPDF()
                } else {
                    Toast.makeText(this, "Permission Not Granted", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }


    private fun downloadPDF() {
      if(checkVersion()){

          downloadPdfNewVersion()
      }
    }



    private fun checkVersion() : Boolean{
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun downloadPdfNewVersion(){

        lifecycleScope.launch {
            val displayName = Constants.displayName // Replace with your desired file name
            val mimeType = Constants.mimeType // Replace with the MIME type of your PDF file
            val contentResolver = applicationContext.contentResolver
            // Permission granted, download the PDF file
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri == null) {
                // Failed to create the file
                binding?.downloadingIcon?.visibility= View.GONE
                Toast.makeText(applicationContext, "Failed to create file", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Write the PDF file to the new file using an OutputStream
            try {
                val outputStream = contentResolver.openOutputStream(uri)
                val pdfUrl = Constants.pdfDownloadURL// Replace with your PDF URL
                val pdfStream = URL(pdfUrl).openStream()
                pdfStream.use { input ->
                    outputStream?.use { output ->
                        input.copyTo(output)
                    }
                }
                binding?.downloadingIcon?.visibility= View.GONE
                binding?.downloadedIcon?.visibility = View.VISIBLE
                Toast.makeText(applicationContext, "PDF downloaded successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Failed to write the file
                e.printStackTrace()
                binding?.downloadingIcon?.visibility= View.GONE
                Toast.makeText(applicationContext, "Failed to write file", Toast.LENGTH_SHORT).show()
                contentResolver.delete(uri, null, null)
                return@launch
            }
        }
    }



}