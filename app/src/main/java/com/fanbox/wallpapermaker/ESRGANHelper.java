package com.fanbox.wallpapermaker;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ESRGANHelper {
    private Interpreter tflite;

    public ESRGANHelper(Context context, String modelPath) throws IOException {
        // 初始化 TFLite 模型
        tflite = new Interpreter(loadModelFile(context, modelPath));
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        FileInputStream inputStream = new FileInputStream(context.getAssets().openFd(modelPath).getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = context.getAssets().openFd(modelPath).getStartOffset();
        long declaredLength = context.getAssets().openFd(modelPath).getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public Bitmap enhanceImage(Bitmap inputBitmap) {
        // 1. 将输入图像调整到模型要求的尺寸（如 50x50）
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(inputBitmap, 50, 50, true);

        // 2. 转换为浮点数组
        float[] inputData = preprocessBitmapToFloatArray(resizedBitmap, 50, 50);

        // 3. 创建输入 TensorBuffer
        TensorBuffer inputBuffer = TensorBuffer.createFixedSize(new int[]{1, 50, 50, 3}, org.tensorflow.lite.DataType.FLOAT32);
        inputBuffer.loadArray(inputData);

        // 4. 创建输出 TensorBuffer
        TensorBuffer outputBuffer = TensorBuffer.createFixedSize(new int[]{1, 200, 200, 3}, org.tensorflow.lite.DataType.FLOAT32);

        // 5. 推理
        tflite.run(inputBuffer.getBuffer(), outputBuffer.getBuffer());

        // 6. 将输出转换回 Bitmap
        return convertOutputToBitmap(outputBuffer);
    }

    /**
     * 将 Bitmap 转换为浮点数组，并进行归一化处理。
     */
    private float[] preprocessBitmapToFloatArray(Bitmap bitmap, int width, int height) {
        int[] intValues = new int[width * height];
        float[] floatValues = new float[width * height * 3];

        // 提取像素数据
        bitmap.getPixels(intValues, 0, width, 0, 0, width, height);

        // 转换为浮点并归一化到 [0, 1]
        for (int i = 0; i < intValues.length; i++) {
            int pixel = intValues[i];
            floatValues[i * 3] = ((pixel >> 16) & 0xFF) / 255.0f; // R
            floatValues[i * 3 + 1] = ((pixel >> 8) & 0xFF) / 255.0f; // G
            floatValues[i * 3 + 2] = (pixel & 0xFF) / 255.0f; // B
        }
        return floatValues;
    }
    public Bitmap convertOutputToBitmap(TensorBuffer outputBuffer) {
        // 从 TensorBuffer 获取输出数据
        float[] outputData = outputBuffer.getFloatArray();

        // 获取输出的宽高（根据模型的实际输出尺寸调整）
        int width = 200; // 假设输出宽度
        int height = 200; // 假设输出高度

        // 创建空白 Bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // 遍历每个像素点，将输出数据映射为像素值
        int[] pixelArray = new int[width * height];
        for (int i = 0; i < pixelArray.length; i++) {
            float value = outputData[i]; // 获取每个像素的预测值
            int color = (int) (value * 255); // 假设值范围是 [0, 1]，映射到 [0, 255]
            pixelArray[i] = 0xFF << 24 | (color << 16) | (color << 8) | color; // ARGB 格式
        }

        // 将像素数组写入 Bitmap
        bitmap.setPixels(pixelArray, 0, width, 0, 0, width, height);

        return bitmap;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
        }
    }
}
