package com.example.hellinproject.camera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.hellinproject.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link CameraFragment#newInstance} factory method to
 * create an instance of this fragment.
 */

@SuppressLint("ValidFragment")
public class CameraFragment extends Fragment {
    public static final String TAG = "[IC]CameraFragment";

    private ConnectionCallback connectionCallback;
    private ImageReader.OnImageAvailableListener imageAvailableListener;
    private Size inputSize;
    private String cameraId;

    private com.example.hellinproject.camera.AutoFitTextureView autoFitTextureView = null;

    private HandlerThread backgroundThread = null;
    private Handler backgroundHandler = null;

    private Size previewSize;
    private int sensorOrientation;

    // 카메라를 획득하는 과정은 여러 프로세스가 동시에 작업할 수 없는 임계 영역에 속하는 것
    // 따라서 세마포어를 사용하여 하나의 프로세스만 공유 자원에 접근할 수 있도록 제어
    private final Semaphore cameraOpenCloseLock = new Semaphore(1);

    private CameraDevice cameraDevice;
    private CaptureRequest.Builder previewRequestBuilder;
    private ImageReader previewReader;
    private CameraCaptureSession captureSession;

    private CameraFragment(final ConnectionCallback callback,
                           final ImageReader.OnImageAvailableListener imageAvailableListener,
                           final Size inputSize,
                           final String cameraId) {
        this.connectionCallback = callback;
        this.imageAvailableListener = imageAvailableListener;
        this.inputSize = inputSize;
        this.cameraId = cameraId;
    }

    // CameraFragment 인스턴스를 반환하는 팩토리 메서드
    // ConnectionCallback : 카메라가 연결된 후 호출할 콜백 함수를 선언하는 인터페이스
    // onImageAvailableListener : 카메라의 다음 프레임 이미지가 준비 완료되었을 때 호출되는 콜백 함수를 정의하는 인터페이스
    // => ImageReader 클래스에 정의되어 있으며, 이미지가 준비되면 onImageAvailable() 함수로 ImageReader를 전달,
    // MainActivity는 이 ImageReader를 전달받아 이미지를 얻어서 추론에 활용
    // Size : 모델 입력 이미지의 가로세로 크기가 전달
    // CameraFragment에는 기기의 카메라가 제공하는 여러 해상도 중 최적의 해상도를 선택하는 로직이 있는데, 그 로직에서 이 값을 바탕으로 최적의 해상도를 구함
    // String : 카메라 ID를 전달, 전면 카메라와 후면 카메라 등 다양한 화각의 카메라 중 적절한 것을 선택하여 그 ID를 String 변수를 통해 전달
    public static CameraFragment newInstance(
            final ConnectionCallback callback,
            final ImageReader.OnImageAvailableListener imageAvailableListener,
            final Size inputSize,
            final String cameraId) {
        return new CameraFragment(callback, imageAvailableListener, inputSize, cameraId);
    }

    // LayoutInflater : 프래그먼트의 View를 객체화
    // ViewGroup : 프래그먼트가 추가될 부모 뷰
    // Bundle : 재생성될 때 기존 상태가 저장되어 있음
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // XML 레이아웃 파일을 인스턴스화하여 반환
       return inflater.inflate(R.layout.activity_camera, container, false);
    }

    // View 생성이 완료되면 호출되는 콜백 함수
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        autoFitTextureView = view.findViewById(R.id.autoFitTextureView);
    }

    // 카메라 연결을 시작하는 함수
    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();

        // autoFitTextureView.setSurfaceTextureListener() 함수를 호출하여 surfaceTextureListener를 등록하면
        // autoFitTextureView가 화면에 출력된 뒤 SurfaceTexture의 상태가 변할 때 콜백 함수를 호출받을 수 있음
        // SurfaceView : 카메라 미리보기가 연속적으로 생성하는 이미지 스트림을 받는 객체로, TextureView 안에서 생성됨
        if(!autoFitTextureView.isAvailable())
            autoFitTextureView.setSurfaceTextureListener(surfaceTextureListener);
        else
            openCamera(autoFitTextureView.getWidth(), autoFitTextureView.getHeight());
    }

    // onPause() 함수가 호출되면 화면이 꺼지거나 프래그먼트가 전환된 것이므로 카메라 연결을 해제하도록 closeCamera() 함수를 호출함
    @Override
    public void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    // 백그라운드 스레드 생성
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("ImageListener");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    // 백그라운드 스레드 종료
    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
    }

    // onSurfaceTextureAvailable() : SurfaceTexture가 준비되면 호출됨, 이 시점에 openCamera() 함수를 호출하여 카메라를 연결할 수 있음
    // onSurfaceTextureSizeChanged() : SurfaceTexture의 버퍼 크기가 변경될 때 호출됨
    // => 가로나 세로 크기가 바뀌면 이에 맞추어 적절히 화면을 회전시키기 위해 configureTransform() 함수를 호출함
    // onSurfaceTextureDestroyed() : SurfaceTexture가 소멸될 때 호출
    // onSurfaceTextureUpdated() : SurfaceTexture의 updateTexImage() 함수가 호출되어 텍스터 이미지가 업데이트되었을 때 호출
    private final TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(
                        final SurfaceTexture texture, final int width, final int height) {
                    openCamera(width, height);
                }

                @Override
                public void onSurfaceTextureSizeChanged(
                        final SurfaceTexture texture, final int width, final int height) {
                    configureTransform(width, height);
                }

                @Override
                public boolean onSurfaceTextureDestroyed(final SurfaceTexture texture) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(final SurfaceTexture texture) {
                }
    };

    // 카메라를 연결하는 함수
    @SuppressLint("MissingPermission")
    private void openCamera(final int width, final int height) {
        final Activity activity = getActivity();
        final CameraManager manager =
                (CameraManager)activity.getSystemService(Context.CAMERA_SERVICE);

        // 카메라 출력 크기와 방향을 구함
        setupCameraOutputs(manager);
        // 화면을 적절히 회전시킴
        configureTransform(width, height);

        // 연결에 성공하거나 실패하여 연결 과정이 끝날 때 세마포어를 해제
        // 세마포어를 획득했다면 CameraManager의 openCamera() 함수를 호출하고,
        // 얻지 못했다면 무한히 기다리지 않도록 tryAcquire() 함수를 사용하여 2,500밀리초 동안만 기다리게 함
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                Toast.makeText(getContext(),
                        "Time out waiting to lock camera opening.",
                        Toast.LENGTH_LONG).show();
                activity.finish();
            } else {
                manager.openCamera(cameraId, stateCallback, backgroundHandler);
//                manager.openCamera(cameraId, stateCallback, null);
            }
        } catch (final InterruptedException | CameraAccessException e) {
            e.printStackTrace();
        }
    }

    // 카메라 출력 크기와 방향을 설정하는 함수
    // 카메라 출력 값의 가로세로 크기와 센서의 회전 방향을 계산함
    private void setupCameraOutputs(CameraManager manager) {
        try {
            final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

            // StreamConfigurationMap : 카메라 출력과 관련된 다양한 정보를 가지고 있는 클래스
            final StreamConfigurationMap map =characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

            // getOutputSizes() : 카메라가 지원하는 모든 가로세로 크기를 Size[] 타입으로 반환받을 수 있음
            // 이렇게 얻은 카메라의 지원 출력 크기와 생성자에서 전달받은 딥러닝 모델 입력 이미지의 가로세로 크기를 chooseOptimalSize() 함수에 전달하면
            // 최적의 카메라 출력 크기를 구할 수 있음
            previewSize = chooseOptimalSize(
                    map.getOutputSizes(SurfaceTexture.class),
                    inputSize.getWidth(),
                    inputSize.getHeight());

            // previewSize가 결정되었으면 이를 바탕으로 autoFitTextureView의 크기를 결정
            final int orientation = getResources().getConfiguration().orientation;
            // 현재 화면이 가로 모드라면 previewSize의 가로, 세로 값을 그대로 전달
            // 세로 모드라면 가로, 세로 값을 서로 바꾸어 전달
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                autoFitTextureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
            } else {
                autoFitTextureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
            }
        } catch (final CameraAccessException cae) {
            cae.printStackTrace();
        }

        // 결정된 크기를 알려주기 위해 previewSize와 sensorOrientation 값을 생성자에서 전달받은 함수에 전달
        connectionCallback.onPreviewSizeChosen(previewSize, sensorOrientation);
    }

    // 가로세로 크기가 변경되면 호출하는 함수
    // 액티비티의 Display()에서 getRotation() 함수로 현재 화면의 회전을 얻어 90도, 180도, 270도 회전이 필요한 경우,
    // autoFitTextureView에 회전 연산을 적용하는 함수
    private void configureTransform(final int viewWidth, final int viewHeight) {
        final Activity activity = getActivity();
        if (null == autoFitTextureView || null == previewSize || null == activity) {
            return;
        }

        final int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        final Matrix matrix = new Matrix();
        final RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        final RectF bufferRect =
                new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        final float centerX = viewRect.centerX();
        final float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(
                    centerX - bufferRect.centerX(),
                    centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            final float scale = Math.max(
                    (float) viewHeight / previewSize.getHeight(),
                    (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        autoFitTextureView.setTransform(matrix);
    }

    // 최적의 카메라 출력 크기를 계산하는 함수
    // choices에 전달된 크기 중 width, height 값과 가장 일치하는 크기를 반환하는 함수
    // 카메라가 지원하는 출력 크기 배열에 전달받은 width, height 값과 정확히 일치하는 Size가 있다면 그 값을 바로 반환
    protected Size chooseOptimalSize(final Size[] choices, final int width, final int height) {
        final int minSize = Math.min(width, height);
        final Size desiredSize = new Size(width, height);

        final List<Size> bigEnough = new ArrayList<Size>();
        final List<Size> tooSmall = new ArrayList<Size>();
        for (final Size option : choices) {
            if (option.equals(desiredSize)) {
                return desiredSize;
            }
            // 만약 정확히 일치하는 값이 없다면 가로와 세로 중 작은 크기를 기준으로 하여,
            // 가로와 세로 모두 이 기준 값보다 큰 Size는 bigEnough 리스트에,
            // 그렇지 않은 size는 tooSmall 리스트에 추가
            if (option.getHeight() >= minSize && option.getWidth() >= minSize) {
                bigEnough.add(option);
            } else {
                tooSmall.add(option);
            }
        }

        // bigEnough 리스트가 비어 있지 않다면 리스트의 원소 중 넓이가 가장 작은 Size를 반환하고,
        // 리스트가 비어있다면 tooSmall 리스트의 원소 중 넓이가 가장 넓은 Size를 반환
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            return Collections.max(tooSmall, new CompareSizesByArea());
        }
    }

    // 카메라가 연결되었는지 알 수 있고, 성공적으로 연결되었다면 미리보기 세션을 생성
    // stateCallback이 호출되었다면 성공이든 실패든 연결 프로세스가 끝난 것이므로 세마포어를 해제함
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        // 카메라 연결에 성공했을 시 호출
        // 전달된 CameraDevice는 앞으로도 계속 접근이 필요하기 때문에 멤버 변수로 저장하고 미리보기 세션을 생성
        @Override
        public void onOpened(final CameraDevice cd) {
            cameraOpenCloseLock.release();
            cameraDevice = cd;
            createCameraPreviewSession();
        }

        // 연결 종료
        @Override
        public void onDisconnected(final CameraDevice cd) {
            cameraOpenCloseLock.release();
            cd.close();
            cameraDevice = null;
        }

        // 에러 발생
        @Override
        public void onError(final CameraDevice cd, final int error) {
            cameraOpenCloseLock.release();
            cd.close();
            cameraDevice = null;
            final Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }
    };

    // 미리보기 세션
    // 카메라 프레임을 기기 화면을 통해 보여주기 위한 것
    private void createCameraPreviewSession() {
        try {
            final SurfaceTexture texture = autoFitTextureView.getSurfaceTexture();
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());

            final Surface surface = new Surface(texture);

            previewReader = ImageReader.newInstance(previewSize.getWidth(),
                    previewSize.getHeight(), ImageFormat.YUV_420_888, 2);
            previewReader.setOnImageAvailableListener(imageAvailableListener,
                    backgroundHandler);

            previewRequestBuilder = cameraDevice.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);
            previewRequestBuilder.addTarget(previewReader.getSurface());

            previewRequestBuilder.set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            previewRequestBuilder.set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
            previewRequestBuilder.set(
                    CaptureRequest.FLASH_MODE,
                    CameraMetadata.FLASH_MODE_TORCH);

            cameraDevice.createCaptureSession(
                    Arrays.asList(surface, previewReader.getSurface()),
                    sessionStateCallback,
                    null);
        } catch (final CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final CameraCaptureSession.StateCallback sessionStateCallback =
            new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(final CameraCaptureSession cameraCaptureSession) {
            if (null == cameraDevice) {
                return;
            }

            captureSession = cameraCaptureSession;
            try {
                captureSession.setRepeatingRequest(previewRequestBuilder.build(),
                        null, backgroundHandler);
            } catch (final CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(final CameraCaptureSession cameraCaptureSession) {
            Toast.makeText(getActivity(), "CameraCaptureSession Failed", Toast.LENGTH_SHORT).show();
        }
    };

    // cameraOpenCloseLock 세마포어를 획득하여 카메라 연결을 해제하는 로직을 임계 영역으로 만들고 각각 close() 함수를 호출하여 카메라 연결을 해제
    // 카메라 연결이 해제되면 cameraOpenCloseLock을 해제
    private void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();
            if (null != captureSession) {
                captureSession.close();
                captureSession = null;
            }
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != previewReader) {
                previewReader.close();
                previewReader = null;
            }
        } catch (final InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            cameraOpenCloseLock.release();
        }
    }

    // 인터페이스는 onPreviewSizeChosen() 함수 하나로 구성되어 있음
    // 카메라 이미지의 가로세로 크기와 이미지 회전 여부를 전달
    // 가로세로 크기가 확정되었을 때 호출됨
    public interface ConnectionCallback {
        void onPreviewSizeChosen(Size size, int cameraRotation);
    }

    // 넓이를 기준으로 Size 비교
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(final Size lhs, final Size rhs) {
            return Long.signum(
                    (long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}