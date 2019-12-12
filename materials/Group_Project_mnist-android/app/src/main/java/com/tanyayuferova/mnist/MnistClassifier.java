package com.tanyayuferova.mnist;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


class MnistClassifier {
    private static final String TAG = MnistClassifier.class.getSimpleName();
    private static final int FLOAT_TYPE_SIZE = 4;
    private static final int PIXEL_SIZE = 1;
    private static final int OUTPUT_CLASSES_COUNT = 10;

    private Context context;
    private Interpreter interpreter;
    private Boolean isInitialized = false;

    private int inputImageWidth = 0; // will be inferred from TF Lite model.
    private int inputImageHeight = 0; // will be inferred from TF Lite model.
    private int modelInputSize = 0; // will be inferred from TF Lite model.

    public MnistClassifier(Context context) {
        this.context = context;
    }

    public void initialize() {
        try {
            initializeInterpreter();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeInterpreter() throws IOException {
        AssetManager assetManager = context.getAssets();
        ByteBuffer model = loadModelFile(assetManager, "mnist.tflite");

        Interpreter.Options options = new Interpreter.Options();
        options.setUseNNAPI(true);
        Interpreter interpreter = new Interpreter(model, options);

        int[] inputShape = interpreter.getInputTensor(0).shape();
        inputImageWidth = inputShape[1];
        inputImageHeight = inputShape[2];
        modelInputSize = FLOAT_TYPE_SIZE * inputImageWidth * inputImageHeight * PIXEL_SIZE;

        this.interpreter = interpreter;

        isInitialized = true;
        Log.d(TAG, "Initialized TFLite interpreter.");
    }

    private ByteBuffer loadModelFile(AssetManager assetManager, String filename) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(filename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public String[] classify(Bitmap bitmap) {
        Log.d(TAG, "Classify image");

        if (!isInitialized) {
            return new String[]{"TF Lite Interpreter is not initialized yet."};
        }

        Bitmap resizedImage = Bitmap.createScaledBitmap(
            bitmap,
            inputImageWidth,
            inputImageHeight,
            true
        );
        ByteBuffer byteBuffer = convertBitmapToByteBuffer(resizedImage);
        float[][] output = new float[1][OUTPUT_CLASSES_COUNT];

        interpreter.run(byteBuffer, output);

        float[] result = output[0];

        List<Float> resultList = new ArrayList<Float>(result.length);
        List<Float> sorted = new ArrayList<Float>(result.length);
        for (float f : result) {
            resultList.add(f);
            sorted.add(f);
        }
        Collections.sort(sorted, Collections.reverseOrder());

        String[] classifiers = new String[OUTPUT_CLASSES_COUNT];
        for (int i = 0; i < sorted.size(); i++) {
            int index = resultList.indexOf(sorted.get(i));
            classifiers[i] = index + "";
            Log.d(TAG, index + ": " + sorted.get(i));
        }

        return classifiers;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(modelInputSize);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[inputImageWidth * inputImageHeight];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(),
            0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int pixelValue : pixels) {
            if (pixelValue == 0)
                pixelValue = -1;
            int r = (pixelValue >> 16) & 0xFF;
            int g = (pixelValue >> 8) & 0xFF;
            int b = pixelValue & 0xFF;

            // Convert RGB to grayscale and normalize pixel value to [0..1].
            float normalizedPixelValue = 1 - (r + g + b) / 3.0f / 255.0f;
            byteBuffer.putFloat(normalizedPixelValue);
        }

        return byteBuffer;
    }


    void close() {
        interpreter.close();
        Log.d(TAG, "Closed TFLite interpreter.");
    }
}
