//package com.fanbox.wallpapermaker
//
//
//import android.annotation.SuppressLint
//import android.app.Activity
//import android.content.ContentValues
//import android.content.Intent
//import android.graphics.Bitmap
//import android.graphics.drawable.BitmapDrawable
//import android.net.Uri
//import android.os.Build
//import android.os.Bundle
//import android.os.Environment
//import android.provider.MediaStore
//import android.util.Log
//import android.view.View
//import android.widget.ImageView
//import android.widget.ProgressBar
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.lifecycleScope
//import com.bumptech.glide.Glide
//import com.fanbox.TFLiteUtils
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.cancel
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import org.tensorflow.lite.Interpreter
//import java.io.File
//import java.io.FileOutputStream
//import java.io.IOException
//import java.io.OutputStream
//
//
//class ImageEnhanceActivity : AppCompatActivity() {
//    private  val REQUEST_CHOOSE_IMAGE = 1002
//    private var imageUri: Uri? = null
//    private lateinit var interpreter: Interpreter
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_image_enhance)
//        startChooseImageIntentForResult()
//        val mainImageView = findViewById<ImageView>(R.id.imageViewOrigin)
//        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
//        val resultImageView = findViewById<ImageView>(R.id.imageViewResult)
//        mainImageView.setOnClickListener {
//            // 加载输入图像
//
//            lifecycleScope.launch {
//                // 显示进度条
//                withContext(Dispatchers.Main) {
//                    progressBar.visibility = View.VISIBLE
//                }
//
//                try {
//                    // 提取 mainImageView 的 Bitmap
//                    val inputBitmap = getBitmapFromImageView(mainImageView)
//
//                    // 在后台线程中进行处理
//                    val superResBitmap = withContext(Dispatchers.Default) {
//                        // 1. 创建 CPU 版 Interpreter
//                        interpreter = TFLiteUtils.createCpuInterpreter(
//                            this@ImageEnhanceActivity,
//                            "esrgan2.tflite"
//                        )
//                        val shape = interpreter.getInputTensor(0).shape()  // 形如 [1, 50, 50, 3]
//                        Log.e("TFLite", "Model input shape = ${shape.contentToString()}")
//
//                        // 2. 设置切片参数
//                        val tileWidth = shape[1]  // 例如 50
//                        val tileHeight = shape[2] // 例如 50
//                        val scaleFactor = 4  // 超分倍数
//
//                        // 3. 切分大图
//                        val tiles = TFLiteUtils.splitBitmap(inputBitmap, tileWidth, tileHeight)
//                        Log.e("TFLite", "Number of tiles = ${tiles.size}")
//
//                        // 4. 处理每个小块
//                        val superResTiles = tiles.mapIndexed { index, tileData ->
//                            Log.d("TFLite", "Processing tile $index/${tiles.size}")
//                            val inputBuffer = TFLiteUtils.bitmapToInputBuffer(
//                                tileData.bitmap,
//                                tileWidth,
//                                tileHeight
//                            )
//                            val superResBitmap = TFLiteUtils.runInference(
//                                interpreter = interpreter,
//                                inputBuffer = inputBuffer,
//                                inputWidth = tileWidth,
//                                inputHeight = tileHeight,
//                                scaleFactor = scaleFactor
//                            )
//                            // 返回新的 TileData，包含超分后的 Bitmap 及其位置信息
//                            TFLiteUtils.TileData(
//                                bitmap = superResBitmap,
//                                startX = tileData.startX,
//                                startY = tileData.startY,
//                                width = tileData.width * scaleFactor,
//                                height = tileData.height * scaleFactor
//                            )
//                        }
//
//                        // 5. 拼接增强后的小块
//                        val superResBitmapFinal = TFLiteUtils.mergeBitmaps(
//                            tiles = superResTiles,
//                            originalWidth = inputBitmap.width,
//                            originalHeight = inputBitmap.height,
//                            scaleFactor = scaleFactor
//                        )
//
//                        // 释放资源
//                        inputBitmap.recycle()
//                        interpreter.close()
//
//                        superResBitmapFinal
//                    }
//
//                    // 在主线程中更新 UI
//                    withContext(Dispatchers.Main) {
//                        // 隐藏进度条
//                        progressBar.visibility = View.GONE
//
//                        // 显示增强后的图像
//                        Glide.with(this@ImageEnhanceActivity)
//                            .load(superResBitmap)
//                            .into(resultImageView)
//                    }
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                    Log.e("TFLite", "Error during image processing: ${e.message}")
//
//                    // 隐藏进度条并显示错误信息
//                    withContext(Dispatchers.Main) {
//                        progressBar.visibility = View.GONE
//                        // 你可以在这里显示一个 Toast 或 Snackbar 来提示用户
//                    }
//                }
//            }
//
//        }
//
//        findViewById<ImageView>(R.id.imageViewResult).setOnClickListener {
//            saveTransparentImage()
//        }
////                val options = Interpreter.Options()
////                options.setNumThreads(4)
//    }
//
//    /**
//     * 从 ImageView 获取 Bitmap
//     */
//    private fun getBitmapFromImageView(imageView: ImageView): Bitmap {
//        // 确保 ImageView 已经绘制完成
//        imageView.isDrawingCacheEnabled = true
//        val bitmap = Bitmap.createBitmap(imageView.drawingCache)
//        imageView.isDrawingCacheEnabled = false
//        return bitmap
//    }
//
//    private fun startChooseImageIntentForResult() {
//        val intent = Intent()
//        intent.type = "image/*"
//        intent.action = Intent.ACTION_GET_CONTENT
//        startActivityForResult(
//            Intent.createChooser(intent, "Select Picture"),
//            REQUEST_CHOOSE_IMAGE
//        )
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
////        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
////            tryReloadAndDetectInImage()
////        } else
//        if (requestCode == REQUEST_CHOOSE_IMAGE && resultCode == Activity.RESULT_OK) {
//            // In this case, imageUri is returned by the chooser, save it.
//            imageUri = data!!.data
//            Glide.with(this).load(imageUri).into(findViewById(R.id.imageViewOrigin))
//
//            //tryReloadAndDetectInImage()
//        } else {
//            super.onActivityResult(requestCode, resultCode, data)
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        lifecycleScope.cancel()
//    }
//
//
//    @SuppressLint("InlinedApi")
//    private fun saveTransparentImage() {
//        val imageViewResult = findViewById<ImageView>(R.id.imageViewResult)
//        val drawable = imageViewResult.drawable ?: run {
//            Toast.makeText(this, "没有可保存的图像", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        // 确保拿到的是 BitmapDrawable 才能提取 Bitmap
//        val bitmap = (drawable as? BitmapDrawable)?.bitmap ?: run {
//            Toast.makeText(this, "无法解析图像", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        // 为存储的图片取个文件名
//        val fileName = "Subject_Transparent_${System.currentTimeMillis()}.png"
//
//        // 开始保存
//        val outputStream: OutputStream?
//        var savedUri: Uri? = null
//
//        // Android 10+ 推荐使用 MediaStore API
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            // 设置要插入到 MediaStore 的元数据
//            val contentValues = ContentValues().apply {
//                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
//                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
//                // 相当于保存到 DCIM/SubjectSegmentation/ 文件夹 (可自定义)
//                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/SubjectSegmentation")
//                put(MediaStore.Images.Media.IS_PENDING, 1)
//            }
//            val contentUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
//
//            // 插入到 MediaStore
//            val uri = contentResolver.insert(contentUri, contentValues)
//            savedUri = uri
//
//            // 打开输出流
//            outputStream = uri?.let { contentResolver.openOutputStream(it) }
//        } else {
//            // Android 9 (API 28) 及以下，直接写入外部存储，需要确保已有写入权限
//            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
//            val imageFile = File(imagesDir, fileName)
//            outputStream = FileOutputStream(imageFile)
//            savedUri = Uri.fromFile(imageFile)
//        }
//
//        // 将 Bitmap 保存为 PNG
//        outputStream?.use {
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
//        }
//
//        // 如果是 Android 10+，更新 IS_PENDING 状态
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            savedUri?.let { uri ->
//                val contentValues = ContentValues().apply {
//                    put(MediaStore.Images.Media.IS_PENDING, 0)
//                }
//                contentResolver.update(uri, contentValues, null, null)
//            }
//        }
//
//        // 可以在此通知用户保存成功/失败
//        if (savedUri != null) {
//            Toast.makeText(this, "图片已保存: $savedUri", Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
//        }
//    }
//}