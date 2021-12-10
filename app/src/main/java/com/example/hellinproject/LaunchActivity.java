package com.example.hellinproject;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Pair;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.app.Fragment;

import com.example.hellinproject.camera.CameraFragment;
import com.example.hellinproject.tflite.Classifier;
import com.example.hellinproject.utils.YuvToRgbConverter;

import java.io.IOException;
import java.util.Locale;

public class LaunchActivity extends AppCompatActivity {
    public static final String TAG = "[IC]LaunchActivity";

    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    private static final int PERMISSION_REQUEST_CODE = 1;

    private TextView textView;
    private Classifier cls;

    private int previewWidth = 0;
    private int previewHeight = 0;
    private int sensorOrientation = 0;

    private Bitmap rgbFrameBitmap = null;

    private HandlerThread handlerThread;
    private Handler handler;

    private boolean isProcessingFrame = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 프래그먼트의 기본 생성자가 불리지 않기 위해 null을 전달
        super.onCreate(null);
        setContentView(R.layout.activity_launch);

        // 액티비티가 실행되면 카메라에 입력되는 이미지를 보여줄 것이므로 액티비티가 실행되는 동안 화면이 계속 켜져 있어야 함
        // getWindow() : 액티비티의 Window를 얻음
        // addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) : 화면이 꺼지지 않도록 함
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 결과 값을 출력할 TextView를 연결
        textView = findViewById(R.id.textView);

        // 추론에 활용할 Classifier를 생성하여 초기화
        cls = new Classifier(this);
        try {
            cls.init();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // 카메라 권한이 부여되어 있는지 확인
        // checkSelfPermission() 함수에 Manifest.permission.CAMERA를 전달하여 반환 값을 확인
        // PackageManager.PERMISSION_GRANTED가 반환되면 권한이 이미 부여된 것이고, PackageManager.PERMISSION_DENIED가 반환되면 권한이 없는 것
        // 권한이 있는 경우 setFragment() 함수를 호출
        if(checkSelfPermission(CAMERA_PERMISSION)
            == PackageManager.PERMISSION_GRANTED) {
            // activity_launch의 FrameLayout 위치에 들어갈 프래그먼트를 생성하는 setFragment() 함수 호출
            // setFragment() 함수는 카메라를 연결할 CameraFragment를 생성하는데, CameraFragment는 기기의 카메라를 사용하기 때문에 카메라 사용 권한이 필요
            setFragment();
        } else {
            // 권한이 없는 경우 requestPermissions() 함수를 이용하여 사용자에게 권한을 요청
            // requestPermissions() 함수에는 필요한 권한 목록과 요청 결과 확인을 위한 코드를 전달
            // 이때, 요청 코드가 다른 요청 코드와 겹치지 않도록 임의의 값을 정의하여 사용
            // requestPermissions() 함수가 호출되면 사용자에게 권한을 요청하는 팝업이 발생하고, 사용자가 권한을 부여하거나 거부하면 액티비티의 onRequestPermissionsResult() 콜백 함수가 호출됨
            requestPermissions(new String[]{CAMERA_PERMISSION},
                PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected synchronized void onDestroy() {
        cls.finish();
        super.onDestroy();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        handlerThread = new HandlerThread("InferenceThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        super.onPause();
    }

    @Override
    public synchronized void onStart() {
        super.onStart();
    }

    @Override
    public synchronized void onStop() {
        super.onStop();
    }

    // requestCode : requestPermissions() 함수 호출 시 전달했던 코드
    // permissions : 요청했던 권한 목록
    // grantResults : 권한 부여 여부가 각각 전달됨
    @Override
    public void onRequestPermissionsResult(
        int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // requestCode가 우리가 요청한 코드와 일치하는지 확인하고,
        // grantResults를 통해 권한이 부여되었는지 확인하여
        // setFragment() 함수를 수행
        if(requestCode == PERMISSION_REQUEST_CODE) {
            if(grantResults.length > 0 && allPermissionsGranted(grantResults)) {
                setFragment();
            } else {
                // 권한이 부여되지 않았다면 작업을 계속하지 않고 사용자에게 권한이 거부되었음을 알림
                Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
            }
        }
        // requestPermissions()에서 권한을 요청할 때 카메라 권한만 요청했지만 여러 권한이 한 번에 요청될 수도 있음
        // 때문에 permissions와 grantResults 변수는 각각 배열 형태로 반환됨
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // 모든 권한이 부여되었는지 확인하기 위해 allPermissionsGranted() 함수를 작성하여 배열 내 모든 권한이 PERMISSION_GRANTED 상태인지 확인
    private boolean allPermissionsGranted(final int[] grantResults) {
        for (int result : grantResults) {
        if (result != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
    }
        return true;
    }

    // 액티비티에 프래그먼트를 추가
    // 주요 동작 : CameraFragment의 newInstance() 함수를 호출하여 프래그먼트의 인스턴스를 얻고, FramgeLayout을 이 프래그먼트로 대체
    protected void setFragment() {
        // 최적의 카메라 해상도를 구하는 데 기준이 되는 크기를 전달
        // 따라서 모델의 입력 이미지 크기를 기준으로 최적의 카메라 해상도를 구하기 위해 Size에 모델의 입력 이미지 크기를 전달
        Size inputSize = cls.getModelInputSize();
        // chooseCamera() 함수를 호출하여 카메라 ID 값을 구함
        String cameraId = chooseCamera();

        // 성공적으로 위의 두 값을 구했다면 newInstance() 함수를 호출
        if(inputSize.getWidth() > 0 && inputSize.getHeight() > 0 && !cameraId.isEmpty()) {
            Fragment fragment = CameraFragment.newInstance(
                    // ConnectionCallback() 함수
                    // 가로세로 크기가 확정되었을 때 호출되므로 전달받은 가로세로 크기와 센서 방향을 멤버 변수로 전달
                    // 센서 방향은 전달받은 카메라의 회전 값에서 기기 화면의 회전 값을 빼서 구할 수 있음
                    (size, rotation) -> {
                previewWidth = size.getWidth();
                previewHeight = size.getHeight();
                sensorOrientation = rotation - getScreenOrientation();
            },
            // OnImageAvailableListener
            reader->processImage(reader),
            inputSize,
            cameraId);

            Log.d(TAG, "inputSize : " + cls.getModelInputSize() +
                    "sensorOrientation : " + sensorOrientation);
            // getFragmentManager() 함수를 호출하여 얻은 액티비티의 FragmentManager에서 beginTransaction() 함수를 호출하여
            // 프래그먼트 관련 연산을 수행하는 FragmentTransaction을 얻을 수 있음
            // FrameLayout을 CameraFragment로 대체할 것이므로 replace() 함수의 인자로 FrameLayout의 ID와 방금 생성한 fragment를 전달하고,
            // commit() 함수를 호출하여 트랜잭션을 수행
            getFragmentManager().beginTransaction().replace(
                R.id.fragment, fragment).commit();
        } else {
            Toast.makeText(this, "Can't find camera", Toast.LENGTH_SHORT).show();
        }
    }

    // 기기에서 적절한 카메라를 선택하여 카메라 ID를 반환하는 함수
    // 액티비티에서 getSystemService() 함수로 CameraManager를 얻어 getCameraIdList() 함수를 호출하면 기기에 포함된 카메라의 ID를 얻을 수 있음
    // 카메라 ID를 getCameraCharacteristics() 함수에 전달하면 그에 해당하는 카메라의 특성을 알 수 있음
    private String chooseCamera() {
        final CameraManager manager =
            (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                final CameraCharacteristics characteristics =
                    manager.getCameraCharacteristics(cameraId);

                // LENS_FACING : 카메라의 방향
                final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                // LENS_FACING_BACK : 후면 카메라 (1)
                // LENS_FACING_FRONT : 전면 카메라 (0)
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
//                    cameraId = "0";
                    Log.d(TAG, String.valueOf(facing));
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        return "";
    }

    // 화면의 회전 여부를 확인하는 함수
    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
            return 270;
            case Surface.ROTATION_180:
            return 180;
            case Surface.ROTATION_90:
            return 90;
            default:
            return 0;
        }
    }

    // 전달받은 이미지를 ARGB_8888 Bitmap으로 변환하여 딥러닝 모델로 추론하는 함수
    protected void processImage(ImageReader reader) {
        // onPreviewSizeChosen() 함수가 호출되기 전이라면 previewWidth와 previewHeight가 0이므로 이미지를 처리하지 않음
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }

        // 처리가 완료된 이미지를 담을 Bitmap이 아직 생성되지 않았다면 Bitmap.createBitmap() 함수를 이용하여 생성
        if(rgbFrameBitmap == null) {
            rgbFrameBitmap = Bitmap.createBitmap(
                previewWidth,
                previewHeight,
                Bitmap.Config.ARGB_8888);
        }

        // 만약 이전 프레임을 아직 처리 중인 경우라면 이미지가 준비되었더라도 처리하지 않음
        if (isProcessingFrame) {
            return;
        }

        isProcessingFrame = true;

        // 이미지를 처리할 준비가 되었다면 ImageReader에서 acquireLatestImage() 함수를 호출하여 버퍼의 최신 이미지를 얻음
        final Image image = reader.acquireLatestImage();
        if (image == null) {
            isProcessingFrame = false;
            return;
        }

        // YUV_420_888 -> ARGB_8888
        // Image 객체와 ARGB_8888 포맷으로 생성된 Bitmap인 rgbFrameBitmap을 전달하면 Image가 rgbFrameBitmap에 저장됨
        YuvToRgbConverter.yuvToRgb(this, image, rgbFrameBitmap);

        runInBackground(() -> {
        if (cls != null && cls.isInitialized()) {
            final Pair<String, Float> output = cls.classify(rgbFrameBitmap, sensorOrientation);

            runOnUiThread(() -> {
                String resultStr = String.format(Locale.ENGLISH,
                "class : %s, prob : %.2f%%",
                output.first, output.second * 100);
                textView.setText(resultStr);
//                Log.d(TAG, resultStr);
            });
        }
        image.close();
        isProcessingFrame = false;
    });
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }
}