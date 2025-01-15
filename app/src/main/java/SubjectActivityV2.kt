//package com.fanbox.wallpapermaker
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
//import android.widget.ImageView
//import android.widget.TextView
//import android.widget.Toast
//import androidx.activity.enableEdgeToEdge
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.view.ViewCompat
//import androidx.core.view.WindowInsetsCompat
//import androidx.lifecycle.lifecycleScope
//import com.bumptech.glide.Glide
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.MainScope
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import org.opencv.android.OpenCVLoader
//import org.opencv.android.Utils
//import org.opencv.core.Core
//import org.opencv.core.CvType
//import org.opencv.core.Mat
//import org.opencv.core.Rect
//import org.opencv.core.Scalar
//import org.opencv.imgproc.Imgproc
//import java.io.File
//import java.io.FileOutputStream
//import java.io.OutputStream
//
//
//class SubjectActivityV2 : AppCompatActivity() {
//
//    private  val REQUEST_CHOOSE_IMAGE = 1002
//    private var imageUri: Uri? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContentView(R.layout.activity_subject)
//
//        OpenCVLoader.initDebug();
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
//
//        startChooseImageIntentForResult()
//
//        findViewById<ImageView>(R.id.imageViewOrigin).setOnClickListener {
//            startChooseImageIntentForResult()
//        }
//        findViewById<ImageView>(R.id.imageViewResult).setOnClickListener {
//            saveTransparentImage()
//        }
//        findViewById<TextView>(R.id.textViewGoEnhance).setOnClickListener {
//            startActivity(Intent(this@SubjectActivityV2, ImageEnhanceActivity::class.java))
//        }
//
//
//    }
//
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
//            //create bitmap from imageUri
//
//            Glide.with(this).load(imageUri).into(findViewById(R.id.imageViewOrigin))
//            MainScope().launch {
//                processImage()
//            }
//
//            //tryReloadAndDetectInImage()
//        } else {
//            super.onActivityResult(requestCode, resultCode, data)
//        }
//    }
//
//    private suspend fun processImage() {
//        val bitmap = withContext(Dispatchers.Default) {
//            val bitmap =
//                MediaStore.Images.Media.getBitmap(this@SubjectActivityV2.contentResolver, imageUri)
//
//
//            val srcMat = Mat()
//            Utils.bitmapToMat(bitmap, srcMat)
//            // Bitmap -> Mat 后通常是 RGBA (CV_8UC4)，需要转成 BGR (CV_8UC3)
//            Imgproc.cvtColor(srcMat, srcMat, Imgproc.COLOR_RGBA2BGR);
//
//// 创建一个空的遮罩 Mat，大小与 srcMat 相同，类型为 CV_8UC1
//            val mask = Mat(srcMat.size(), CvType.CV_8UC1, Scalar(Imgproc.GC_PR_BGD.toDouble()))
//
//
//// 准备输出用的背景模型和前景模型（必须是 1×65 的 Mat）
//            val bgModel = Mat(1, 65, CvType.CV_64FC1, Scalar(0.0))
//            val fgModel = Mat(1, 65, CvType.CV_64FC1, Scalar(0.0))
//
//            val width = srcMat.cols()
//            val height = srcMat.rows()
//            val rect: Rect = Rect(width / 10, height / 10, width * 7 / 10, height * 7 / 10)
//
//            val iterCount = 5
//
//            Imgproc.grabCut(
//                srcMat,  // 原图
//                mask,  // 掩码
//                rect,  // 主体区域矩形
//                bgModel,  // 背景模型
//                fgModel,  // 前景模型
//                iterCount,  // 迭代次数
//                Imgproc.GC_INIT_WITH_RECT
//            )
//
//            val foregroundMask = Mat(srcMat.size(), CvType.CV_8UC1)
//            Core.compare(mask, Scalar(Imgproc.GC_PR_FGD.toDouble()), foregroundMask, Core.CMP_EQ)
//
//
//// 也可写成：
//// for each pixel in mask:
////   if value == GC_FGD or GC_PR_FGD => 255
////   else => 0
//
//            // 用前景 Mask 和原图做位与，得到最后抠出图
//            val foreground = Mat(srcMat.size(), CvType.CV_8UC3, Scalar.all(0.0))
//            srcMat.copyTo(foreground, foregroundMask)
//
//
//            // 如果需要继续转回 Bitmap
//            val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//            Utils.matToBitmap(foreground, resultBitmap)
//            return@withContext resultBitmap
//        }
//        withContext(Dispatchers.Main){
//            Glide.with(this@SubjectActivityV2).load(bitmap).into(findViewById(R.id.imageViewResult))
//        }
//
//    }
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