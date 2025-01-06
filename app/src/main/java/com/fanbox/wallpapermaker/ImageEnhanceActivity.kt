package com.fanbox.wallpapermaker


import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import org.tensorflow.lite.Interpreter
import java.io.IOException


class ImageEnhanceActivity : AppCompatActivity() {
    private var esrganHelper: ESRGANHelper? = null
    private  val REQUEST_CHOOSE_IMAGE = 1002
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_enhance)
        startChooseImageIntentForResult()
        val mainImageView = findViewById<ImageView>(R.id.imageViewOrigin)
        mainImageView.setOnClickListener {
            // 加载输入图像


            // 提取 mainImageView 的 Bitmap
            mainImageView.isDrawingCacheEnabled = true
            val inputBitmap = Bitmap.createBitmap(mainImageView.drawingCache)
            mainImageView.isDrawingCacheEnabled = false
            try {
                // 初始化 ESRGAN Helper
                esrganHelper = ESRGANHelper(this, "esrgan2.tflite")

                // 增强图像分辨率
                val outputBitmap = esrganHelper?.enhanceImage(inputBitmap)

                // 显示增强后的图像
                Glide.with(this).load(outputBitmap).into(findViewById(R.id.imageViewResult))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
//                val options = Interpreter.Options()
//                options.setNumThreads(4)
    }

    private fun startChooseImageIntentForResult() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "Select Picture"),
            REQUEST_CHOOSE_IMAGE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
//            tryReloadAndDetectInImage()
//        } else
        if (requestCode == REQUEST_CHOOSE_IMAGE && resultCode == Activity.RESULT_OK) {
            // In this case, imageUri is returned by the chooser, save it.
            imageUri = data!!.data
            Glide.with(this).load(imageUri).into(findViewById(R.id.imageViewOrigin))

            //tryReloadAndDetectInImage()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        esrganHelper?.close()
    }
}