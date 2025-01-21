package com.fanbox.wallpapermaker


import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


class MakerActivity : AppCompatActivity() {
    private val REQUEST_CHOOSE_IMAGE = 1002
    private val REQUEST_CHOOSE_IMAGE_BG = 1001
    private var imageUri: Uri? = null
    private var loadingLayout : CardView? = null
    private var rootContainer: FrameLayout? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_maker)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        rootContainer = findViewById(R.id.root_container);
        loadingLayout = findViewById(R.id.loading_layout)
        //startChooseImageIntentForResult()

        findViewById<Button>(R.id.btn_add_image).setOnClickListener {
            startChooseImageIntentForResult()
        }
        findViewById<Button>(R.id.btn_add_bg).setOnClickListener {
            startChooseImageIntentForResult(true)
        }
        findViewById<Button>(R.id.btn_save_result).setOnClickListener {
            if (bgView!=null){
                //saveBgViewArea()
                saveRootContainerClipped()
            }else{
                saveRootContainerClipped()
            }

        }
        findViewById<View>(R.id.previous_step).setOnClickListener {
            rootContainer?.removeViewAt(rootContainer!!.childCount-1)
            if (rootContainer?.childCount==0){
                bgView = null
            }
        }

        findViewById<View>(R.id.restart).setOnClickListener{
            rootContainer?.removeAllViews()
            bgView = null
        }
    }

    private fun startChooseImageIntentForResult(isBg : Boolean = false) {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(intent, "Select Picture"),
          if (isBg)  REQUEST_CHOOSE_IMAGE_BG else REQUEST_CHOOSE_IMAGE
        )
    }

    private fun addSelectableImage(imageUri: Any) {
        // 1. 动态加载自定义布局（包含图片 & selector背景）
        val itemView: View = LayoutInflater.from(this)
            .inflate(R.layout.item_selectable_image, rootContainer, false)

        // 2. 设置居中布局参数
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.CENTER // 让itemView居中

        itemView.layoutParams = layoutParams
        // 2. 找到内部的 ImageView，设置图片资源
        val ivContent = itemView.findViewById<ImageView>(R.id.iv_content)
        Glide.with(this).load(imageUri).into(ivContent)


        // 4. 设置点击事件：切换 selected 状态
        itemView.setOnClickListener { v ->
            val newSelectedState = !v.isSelected
            v.isSelected = newSelectedState
            if (v.isSelected){
                itemView.setOnTouchListener(MultiTouchListenerWithRotate(itemView))
            }else{
                itemView.setOnTouchListener(null)
            }
        }

        // 5. 添加到父容器中
        rootContainer!!.addView(itemView)
    }

    var bgView :View? = null
    private fun addBgImage(imageUri: Any) {
        // 1. 动态加载自定义布局（包含图片 & selector背景）
        if (bgView != null){
            rootContainer?.removeView(bgView)
        }
        val itemView: View = LayoutInflater.from(this)
            .inflate(R.layout.item_selectable_image, rootContainer, false)
        bgView = itemView

        // 2. 设置居中布局参数
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.CENTER // 让itemView居中

        itemView.layoutParams = layoutParams

        val ivContent = itemView.findViewById<ImageView>(R.id.iv_content)
        Glide.with(this).load(imageUri).into(ivContent)


        // 4. 设置点击事件：切换 selected 状态
//        itemView.setOnClickListener { v ->
//            val newSelectedState = !v.isSelected
//            v.isSelected = newSelectedState
//            if (v.isSelected){
//                itemView.setOnTouchListener(MultiTouchListener(itemView))
//            }else{
//                itemView.setOnTouchListener(null)
//            }
//        }

        // 5. 添加到父容器中
        rootContainer!!.addView(itemView,0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
//            tryReloadAndDetectInImage()
//        } else
        if (requestCode == REQUEST_CHOOSE_IMAGE && resultCode == Activity.RESULT_OK) {
            // In this case, imageUri is returned by the chooser, save it.
            imageUri = data!!.data
            loadingLayout?.visibility = View.VISIBLE
            //imageUri?.let { addSelectableImage(it) }
            //Glide.with(this).load(imageUri).into(findViewById(R.id.imageViewOrigin))

            processImage()
            //tryReloadAndDetectInImage()
        } else if (requestCode == REQUEST_CHOOSE_IMAGE_BG && resultCode == Activity.RESULT_OK) {
            imageUri = data!!.data
            addBgImage(imageUri!!)
        }else{
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun processImage() {

        val inputImage: InputImage = InputImage.fromFilePath(this@MakerActivity, imageUri!!)

        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()

        val segmenter = SubjectSegmentation.getClient(options)
        segmenter.process(inputImage)
            .addOnSuccessListener { result ->
                // Task completed successfully
                // ...
                loadingLayout?.visibility = View.GONE
                val foregroundBitmap = result.foregroundBitmap
                foregroundBitmap?.let { addSelectableImage(it) }
                //Glide.with(this).load(foregroundBitmap).into(findViewById(R.id.imageViewResult))

            }
            .addOnFailureListener { e ->
                // Task failed with an exception
                // ...
            }
    }

    private fun saveBgViewArea() {
        if (bgView == null) {
            Toast.makeText(this, "请先添加背景图", Toast.LENGTH_SHORT).show()
            return
        }
        val w = bgView!!.width
        val h = bgView!!.height
        if (w <= 0 || h <= 0) {
            Toast.makeText(this, "bgView 宽高无效", Toast.LENGTH_SHORT).show()
            return
        }

        // 创建 Bitmap
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 平移，确保只绘制bgView这块区域
        canvas.translate(-bgView!!.x, -bgView!!.y)

        // 绘制 rootContainer
        rootContainer?.draw(canvas)

        // 保存到相册(伪代码，逻辑同之前的 MediaStore 方案)
        save2phone(bitmap)
    }

    private fun getViewTransformedBounds(child: View): RectF {
        val points = floatArrayOf(
            0f, 0f,
            child.width.toFloat(), 0f,
            child.width.toFloat(), child.height.toFloat(),
            0f, child.height.toFloat()
        )

        val matrix = Matrix(child.matrix) // 拷贝child的matrix
        matrix.postTranslate(child.x, child.y) // 平移

        matrix.mapPoints(points)

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        for (i in points.indices step 2) {
            val x = points[i]
            val y = points[i + 1]
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }

        return RectF(minX, minY, maxX, maxY)
    }

    private fun calculateChildrenBounds(root: ViewGroup): RectF {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child.visibility != View.VISIBLE) continue

            val bounds = getViewTransformedBounds(child)
            if (bounds.left < minX) minX = bounds.left
            if (bounds.top < minY) minY = bounds.top
            if (bounds.right > maxX) maxX = bounds.right
            if (bounds.bottom > maxY) maxY = bounds.bottom
        }

        if (minX == Float.MAX_VALUE) {
            return RectF(0f, 0f, 0f, 0f)
        }
        return RectF(minX, minY, maxX, maxY)
    }


    private fun saveRootContainerClipped() {
        val rootContainer = findViewById<FrameLayout>(R.id.root_container)

        // 1. 获取合并后的 bounding box
        val bounds = calculateChildrenBounds(rootContainer)

        val contentWidth = (bounds.right - bounds.left).toInt()
        val contentHeight = (bounds.bottom - bounds.top).toInt()
        if (contentWidth <= 0 || contentHeight <= 0) {
            Toast.makeText(this, "没有有效内容", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. 创建 Bitmap，并做负向平移以实现“裁剪”
        val bitmap = Bitmap.createBitmap(contentWidth, contentHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        // 平移，确保只绘制 [bounds.left,bounds.top] 到 [bounds.right,bounds.bottom] 的内容
        canvas.translate(-bounds.left, -bounds.top)
        //canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        // 3. rootContainer.draw(canvas)
        rootContainer.draw(canvas)

        // 4. 将这个 bitmap 保存即可 (MediaStore/ FileOutputStream)
        save2phone(bitmap)
    }

    private fun save2phone(bitmap: Bitmap) {
        // 后续就跟你原来的保存逻辑一致，把 bitmap 写到文件即可
        val fileName = "clipped_${System.currentTimeMillis()}.png"
        val outputStream: OutputStream?
        var savedUri: Uri? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/SubjectSegmentation")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val contentUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            savedUri = contentResolver.insert(contentUri, contentValues)
            outputStream = savedUri?.let { contentResolver.openOutputStream(it) }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val imageFile = File(imagesDir, fileName)
            outputStream = FileOutputStream(imageFile)
            savedUri = Uri.fromFile(imageFile)
        }

        outputStream?.use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            savedUri?.let { uri ->
                val cv = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                contentResolver.update(uri, cv, null, null)
            }
        }

        if (savedUri != null) {
            Toast.makeText(this, "已保存: $savedUri", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show()
        }
    }



}