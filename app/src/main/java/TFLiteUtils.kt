//package com.fanbox
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.graphics.Canvas
//import android.graphics.Color
//import android.util.Log
//import org.tensorflow.lite.Interpreter
//import org.tensorflow.lite.gpu.GpuDelegate
//import java.io.FileInputStream
//import java.io.IOException
//import java.lang.Math.min
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//import java.nio.channels.FileChannel
//import kotlin.math.ceil
//
//object TFLiteUtils {
//
//    /**
//     * 从 Assets 中加载 .tflite 模型，转为 MappedByteBuffer
//     *
//     * @param context 上下文
//     * @param modelPath 模型在 assets 中的路径，如 "model.tflite"
//     */
//    @Throws(IOException::class)
//    fun loadModelFile(context: Context, modelPath: String): ByteBuffer {
//        context.assets.openFd(modelPath).use { fileDescriptor ->
//            FileInputStream(fileDescriptor.fileDescriptor).channel.use { fileChannel ->
//                val startOffset = fileDescriptor.startOffset
//                val declaredLength = fileDescriptor.declaredLength
//                return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
//            }
//        }
//    }
//
//    /**
//     * 创建 CPU 版本的 TFLite Interpreter
//     *
//     * @param context 上下文
//     * @param modelPath .tflite 文件在 assets 中的路径
//     */
//    fun createCpuInterpreter(context: Context, modelPath: String): Interpreter {
//        val modelBuffer = loadModelFile(context, modelPath)
//        val options = Interpreter.Options()
//        return Interpreter(modelBuffer, options)
//    }
//
//    /**
//     * 创建 GPU 版本的 TFLite Interpreter
//     *
//     * @param context 上下文
//     * @param modelPath .tflite 文件在 assets 中的路径
//     */
////    fun createGpuInterpreter(context: Context, modelPath: String): Interpreter {
////        val modelBuffer = loadModelFile(context, modelPath)
////
////        val gpuDelegateOptions = GpuDelegate.Options().apply {
////            // 如果模型可以容忍一定精度损失，可设置为 true 以提升推理速度
////            precisionLossAllowed = true
////        }
////        val gpuDelegate = GpuDelegate(gpuDelegateOptions)
////
////        val options = Interpreter.Options().addDelegate(gpuDelegate)
////        return Interpreter(modelBuffer, options)
////    }
//
//    /**
//     * 将 Bitmap 转换为 TFLite 输入所需的 ByteBuffer
//     *
//     * @param bitmap 输入的低分辨率图片
//     * @param inputWidth 模型需要的输入宽
//     * @param inputHeight 模型需要的输入高
//     * @return ByteBuffer, shape = [1, inputHeight, inputWidth, 3]
//     */
//    fun bitmapToInputBuffer(
//        bitmap: Bitmap,
//        inputWidth: Int,
//        inputHeight: Int
//    ): ByteBuffer {
//        // 先将 Bitmap 缩放到指定大小
//        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, false)
//
//        // 每个像素 3 通道，每个通道一个 float（4 bytes）
//        val inputBuffer = ByteBuffer.allocateDirect(4 * inputWidth * inputHeight * 3)
//        inputBuffer.order(ByteOrder.nativeOrder())
//
//        // 遍历每个像素，将其 RGB 归一化到 [0, 1] 并写入
//        for (y in 0 until inputHeight) {
//            for (x in 0 until inputWidth) {
//                val pixel = resizedBitmap.getPixel(x, y)
//                // 提取 RGB 通道并归一化
//                val r = Color.red(pixel) / 255f
//                val g = Color.green(pixel) / 255f
//                val b = Color.blue(pixel) / 255f
//                inputBuffer.putFloat(r)
//                inputBuffer.putFloat(g)
//                inputBuffer.putFloat(b)
//            }
//        }
//        inputBuffer.rewind()
//        return inputBuffer
//    }
//
//    /**
//     * 运行推理，将输入 ByteBuffer 送入 Interpreter 获得超分辨率输出
//     *
//     * @param interpreter TFLite 解释器
//     * @param inputBuffer 已经填充好数据的输入 buffer, shape=[1, inputHeight, inputWidth, 3]
//     * @param inputWidth 输入图像宽度
//     * @param inputHeight 输入图像高度
//     * @param scaleFactor 超分辨率倍数，如 x4
//     * @return 超分后的 Bitmap
//     */
//    fun runInference(
//        interpreter: Interpreter,
//        inputBuffer: ByteBuffer,
//        inputWidth: Int,
//        inputHeight: Int,
//        scaleFactor: Int = 4
//    ): Bitmap {
//        // 输出大小，假设是 x4 超分
//        val outputWidth = inputWidth * scaleFactor
//        val outputHeight = inputHeight * scaleFactor
//
//        // 创建输出缓存 [1, outputHeight, outputWidth, 3]
//        val outputArray = Array(1) {
//            Array(outputHeight) {
//                Array(outputWidth) {
//                    FloatArray(3)
//                }
//            }
//        }
//
//        // 推理
//        interpreter.run(inputBuffer, outputArray)
//
//        // 将 float 数组映射回 Bitmap
//        val outputBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
//        for (y in 0 until outputHeight) {
//            for (x in 0 until outputWidth) {
//                val r = (outputArray[0][y][x][0] * 255f).toInt().coerceIn(0, 255)
//                val g = (outputArray[0][y][x][1] * 255f).toInt().coerceIn(0, 255)
//                val b = (outputArray[0][y][x][2] * 255f).toInt().coerceIn(0, 255)
//                outputBitmap.setPixel(x, y, Color.rgb(r, g, b))
//            }
//        }
//        return outputBitmap
//    }
//
//    data class TileData(
//        val bitmap: Bitmap,
//        val startX: Int,
//        val startY: Int,
//        val width: Int,
//        val height: Int
//    )
//
//
//    /**
//     * 将 Bitmap 切分成多个小块，并记录每个小块在原始图像中的位置
//     *
//     * @param bitmap 原始大图
//     * @param tileWidth 每个小块的宽度
//     * @param tileHeight 每个小块的高度
//     * @return 切分后的 TileData 列表
//     */
//    fun splitBitmap(bitmap: Bitmap, tileWidth: Int, tileHeight: Int): List<TileData> {
//        val tileList = mutableListOf<TileData>()
//        val cols = ceil(bitmap.width.toDouble() / tileWidth).toInt()
//        val rows = ceil(bitmap.height.toDouble() / tileHeight).toInt()
//
//        for (y in 0 until rows) {
//            for (x in 0 until cols) {
//                val startX = x * tileWidth
//                val startY = y * tileHeight
//                val endX = min(startX + tileWidth, bitmap.width)
//                val endY = min(startY + tileHeight, bitmap.height)
//                val w = endX - startX
//                val h = endY - startY
//
//                val tileBmp = Bitmap.createBitmap(bitmap, startX, startY, w, h)
//                tileList.add(TileData(tileBmp, startX, startY, w, h))
//            }
//        }
//        return tileList
//    }
//
//    /**
//     * 将多个小块拼接成一个完整的 Bitmap
//     *
//     * @param tiles 切分后的 TileData 列表
//     * @param originalWidth 原始大图的宽度
//     * @param originalHeight 原始大图的高度
//     * @param scaleFactor 超分辨率倍数
//     * @return 拼接后的增强大图
//     */
//    fun mergeBitmaps(
//        tiles: List<TileData>,
//        originalWidth: Int,
//        originalHeight: Int,
//        scaleFactor: Int
//    ): Bitmap {
//        val scaledWidth = originalWidth * scaleFactor
//        val scaledHeight = originalHeight * scaleFactor
//
//        // 创建目标大图
//        val mergedBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(mergedBitmap)
//
//        for (tileData in tiles) {
//            val tile = tileData.bitmap
//            // 超分缩放小块
//            val scaledTile = Bitmap.createScaledBitmap(
//                tile,
//                tileData.width * scaleFactor,
//                tileData.height * scaleFactor,
//                false
//            )
//            // 计算绘制位置
//            val destX = tileData.startX * scaleFactor
//            val destY = tileData.startY * scaleFactor
//            // 使用 Canvas 绘制到目标大图
//            canvas.drawBitmap(scaledTile, destX.toFloat(), destY.toFloat(), null)
//
//            // 释放资源
//            scaledTile.recycle()
//            tile.recycle()
//        }
//
//        return mergedBitmap
//    }
//
//
//}
