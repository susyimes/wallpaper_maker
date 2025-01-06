package com.fanbox.wallpapermaker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Pair
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class SubjectActivity : AppCompatActivity() {
    private  val REQUEST_CHOOSE_IMAGE = 1002
    private var imageUri: Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_subject)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        startChooseImageIntentForResult()

        findViewById<ImageView>(R.id.imageViewOrigin).setOnClickListener {
            startChooseImageIntentForResult()
        }
        findViewById<ImageView>(R.id.imageViewResult).setOnClickListener {
            saveTransparentImage()
        }
        findViewById<TextView>(R.id.textViewGoEnhance).setOnClickListener {
            startActivity(Intent(this@SubjectActivity, ImageEnhanceActivity::class.java))
        }
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
            processImage()
            //tryReloadAndDetectInImage()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun processImage() {

        val inputImage: InputImage = InputImage.fromFilePath(this@SubjectActivity, imageUri!!)

        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()

        val segmenter = SubjectSegmentation.getClient(options)
        segmenter.process(inputImage)
            .addOnSuccessListener { result ->
                // Task completed successfully
                // ...
                val foregroundBitmap = result.foregroundBitmap
                Glide.with(this).load(foregroundBitmap).into(findViewById(R.id.imageViewResult))

            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                // ...
            }
    }

    @SuppressLint("InlinedApi")
    private fun saveTransparentImage() {
        val imageViewResult = findViewById<ImageView>(R.id.imageViewResult)
        val drawable = imageViewResult.drawable ?: run {
            Toast.makeText(this, "没有可保存的图像", Toast.LENGTH_SHORT).show()
            return
        }

        // 确保拿到的是 BitmapDrawable 才能提取 Bitmap
        val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: run {
            Toast.makeText(this, "无法解析图像", Toast.LENGTH_SHORT).show()
            return
        }

        // 为存储的图片取个文件名
        val fileName = "Subject_Transparent_${System.currentTimeMillis()}.png"

        // 开始保存
        val outputStream: OutputStream?
        var savedUri: Uri? = null

        // Android 10+ 推荐使用 MediaStore API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 设置要插入到 MediaStore 的元数据
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                // 相当于保存到 DCIM/SubjectSegmentation/ 文件夹 (可自定义)
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/SubjectSegmentation")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val contentUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

            // 插入到 MediaStore
            val uri = contentResolver.insert(contentUri, contentValues)
            savedUri = uri

            // 打开输出流
            outputStream = uri?.let { contentResolver.openOutputStream(it) }
        } else {
            // Android 9 (API 28) 及以下，直接写入外部存储，需要确保已有写入权限
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val imageFile = File(imagesDir, fileName)
            outputStream = FileOutputStream(imageFile)
            savedUri = Uri.fromFile(imageFile)
        }

        // 将 Bitmap 保存为 PNG
        outputStream?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        // 如果是 Android 10+，更新 IS_PENDING 状态
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            savedUri?.let { uri ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                contentResolver.update(uri, contentValues, null, null)
            }
        }

        // 可以在此通知用户保存成功/失败
        if (savedUri != null) {
            Toast.makeText(this, "图片已保存: $savedUri", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
        }
    }




}