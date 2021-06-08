package com.test.epmediatest

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL
import com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS
import com.arthenica.mobileffmpeg.ExecuteCallback
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    var videoPath : String = ""
    var imagePath : String = ""

    var extStorageDirectory : String = ""

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkVerify()

        saveImage()

        findViewById<Button>(R.id.btn_pick_video).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "video/*"
            startActivityForResult(intent, 0)
        }

        findViewById<Button>(R.id.btn_pick_image).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }

        findViewById<Button>(R.id.btn_start).setOnClickListener {
            if(videoPath.isNotEmpty() && imagePath.isNotEmpty()){
                val current : String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

                val outputPath = "${Environment.getExternalStorageDirectory()}/${Environment.DIRECTORY_DOWNLOADS}/${current}.mp4"

                startFFMPEG(imagePath, videoPath, outputPath)
            }
        }

    }

    @SuppressLint("UseCompatLoadingForDrawables")
    fun saveImage() {
        val bm = (getDrawable(R.drawable.water) as BitmapDrawable).bitmap
        val wrapper = ContextWrapper(applicationContext)
        val path = wrapper.getDir("Images", Context.MODE_PRIVATE)
        val file = File(path, "water.png")

        try {
            val stream = FileOutputStream(file)
            bm.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e : IOException){
            e.printStackTrace()
        }

        imagePath = Uri.parse(file.absolutePath).toString()

        Log.e(TAG, "saveImage Url : ${Uri.parse(file.absolutePath)}")

    }

    fun checkVerify() : Boolean {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            }
            requestPermissions(
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), 1
            )

            return false
        } else {
            return true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            1 -> {
                if (data != null) {

                    val result = URIPathHelper().getPath(applicationContext, data.data!!)

                    imagePath = result.toString()
                }
            }
            0 -> {
                if (data != null){
                    val result = URIPathHelper().getPath(applicationContext, data.data!!)

                    videoPath = result.toString()
                }
            }
        }
    }

    private fun startFFMPEG(imageUrl : String, videoUrl : String, outputUrl : String) {
        FFmpeg.executeAsync(addWaterMark(imageUrl, videoUrl, outputUrl), object : ExecuteCallback{
            override fun apply(executionId: Long, returnCode: Int) {
                when(returnCode){
                    RETURN_CODE_SUCCESS -> {
                        Log.e(Config.TAG, "Async command execution completed successfully.")
                    }
                    RETURN_CODE_CANCEL -> {
                        Log.e(Config.TAG, "Async command execution cancelled by user.")
                    }
                    else -> {
                        Log.e(Config.TAG, String.format("Async command execution failed with returnCode=%d.", returnCode))
                    }
                }
            }
        })
    }

    private fun addWaterMark(
            imageUrl : String,
            videoUrl : String,
            outputUrl : String
    ) : Array<String?>{
        var command = arrayOfNulls<String>(7)

        command[0] = "-i"
        command[1] = videoUrl
        command[2] = "-i"
        command[3] = imageUrl
        command[4] = "-filter_complex"
        command[5] = "overlay=x=main_w-(overlay_w+10):y=(main_h-overlay_h)/2"
        command[6] = outputUrl

        return command
    }
}