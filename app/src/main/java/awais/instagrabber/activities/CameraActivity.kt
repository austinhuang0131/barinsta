package awais.instagrabber.activities

import android.content.Intent
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayManager.DisplayListener
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import awais.instagrabber.databinding.ActivityCameraBinding
import awais.instagrabber.utils.DownloadUtils
import awais.instagrabber.utils.PermissionUtils
import awais.instagrabber.utils.Utils
import awais.instagrabber.utils.extensions.TAG
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : BaseLanguageActivity() {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var displayManager: DisplayManager
    private lateinit var cameraExecutor: ExecutorService

    private var outputDirectory: DocumentFile? = null
    private var imageCapture: ImageCapture? = null
    private var displayId = -1
    private var cameraProvider: ProcessCameraProvider? = null
    private var lensFacing = 0

    private val cameraRequestCode = 100
    private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
    private val displayListener: DisplayListener = object : DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == this@CameraActivity.displayId) {
                imageCapture?.targetRotation = binding.root.display.rotation
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(LayoutInflater.from(baseContext))
        setContentView(binding.root)
        Utils.transparentStatusBar(this, true, false)
        displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager
        outputDirectory = DownloadUtils.cameraDir
        cameraExecutor = Executors.newSingleThreadExecutor()
        displayManager.registerDisplayListener(displayListener, null)
        binding.viewFinder.post {
            displayId = binding.viewFinder.display.displayId
            updateUi()
            checkPermissionsAndSetupCamera()
        }
    }

    override fun onResume() {
        super.onResume()
        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
        if (!PermissionUtils.hasCameraPerms(this)) {
            PermissionUtils.requestCameraPerms(this, cameraRequestCode)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Redraw the camera UI controls
        updateUi()

        // Enable or disable switching between cameras
        updateCameraSwitchButton()
    }

    override fun onDestroy() {
        super.onDestroy()
        Utils.transparentStatusBar(this, false, false)
        cameraExecutor.shutdown()
        displayManager.unregisterDisplayListener(displayListener)
    }

    private fun updateUi() {
        binding.cameraCaptureButton.setOnClickListener { takePhoto() }
        // Disable the button until the camera is set up
        binding.switchCamera.isEnabled = false
        // Listener for button used to switch cameras. Only called if the button is enabled
        binding.switchCamera.setOnClickListener {
            lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
            // Re-bind use cases to update selected camera
            bindCameraUseCases()
        }
        binding.close.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun checkPermissionsAndSetupCamera() {
        if (PermissionUtils.hasCameraPerms(this)) {
            setupCamera()
            return
        }
        PermissionUtils.requestCameraPerms(this, cameraRequestCode)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraRequestCode) {
            if (PermissionUtils.hasCameraPerms(this)) {
                setupCamera()
            }
        }
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                // Select lensFacing depending on the available cameras
                lensFacing = -1
                if (hasBackCamera()) {
                    lensFacing = CameraSelector.LENS_FACING_BACK
                } else if (hasFrontCamera()) {
                    lensFacing = CameraSelector.LENS_FACING_FRONT
                }
                check(lensFacing != -1) { "Back and front camera are unavailable" }
                // Enable or disable switching between cameras
                updateCameraSwitchButton()
                // Build and bind the camera use cases
                bindCameraUseCases()
            } catch (e: ExecutionException) {
                Log.e(TAG, "setupCamera: ", e)
            } catch (e: InterruptedException) {
                Log.e(TAG, "setupCamera: ", e)
            } catch (e: CameraInfoUnavailableException) {
                Log.e(TAG, "setupCamera: ", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val rotation = binding.viewFinder.display.rotation

        // CameraSelector
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        // Preview
        val preview = Preview.Builder() // Set initial target rotation
            .setTargetRotation(rotation)
            .build()

        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY) // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            .setTargetRotation(rotation)
            .build()
        cameraProvider?.unbindAll()
        cameraProvider?.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)
    }

    private fun takePhoto() {
        if (imageCapture == null) return
        val fileName = simpleDateFormat.format(System.currentTimeMillis()) + ".jpg"
        val mimeType = "image/jpg"
        val photoFile = outputDirectory?.createFile(mimeType, fileName)?.let { it } ?: return
        val outputStream = contentResolver.openOutputStream(photoFile.uri)?.let { it } ?: return
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(outputStream).build()
        imageCapture?.takePicture(
            outputFileOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                @Suppress("UnstableApiUsage")
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    try { outputStream.close() } catch (ignored: IOException) {}
                    val intent = Intent()
                    intent.data = photoFile.uri
                    setResult(RESULT_OK, intent)
                    finish()
                    Log.d(TAG, "onImageSaved: " + photoFile.uri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "onError: ", exception)
                    try { outputStream.close() } catch (ignored: IOException) {}
                }
            }
        )
        // We can only change the foreground Drawable using API level 23+ API
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        //     // Display flash animation to indicate that photo was captured
        //     final ConstraintLayout container = binding.getRoot();
        //     container.postDelayed(() -> {
        //         container.setForeground(new ColorDrawable(Color.WHITE));
        //         container.postDelayed(() -> container.setForeground(null), 50);
        //     }, 100);
        // }
    }

    /**
     * Enabled or disabled a button to switch cameras depending on the available cameras
     */
    private fun updateCameraSwitchButton() {
        try {
            binding.switchCamera.isEnabled = hasBackCamera() && hasFrontCamera()
        } catch (e: CameraInfoUnavailableException) {
            binding.switchCamera.isEnabled = false
        }
    }

    /**
     * Returns true if the device has an available back camera. False otherwise
     */
    @Throws(CameraInfoUnavailableException::class)
    private fun hasBackCamera(): Boolean {
        return if (cameraProvider == null) false else cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /**
     * Returns true if the device has an available front camera. False otherwise
     */
    @Throws(CameraInfoUnavailableException::class)
    private fun hasFrontCamera(): Boolean {
        return if (cameraProvider == null) {
            false
        } else cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }
}