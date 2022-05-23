package cash.z.ecc.android.ui.scan

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import cash.z.ecc.android.R
import cash.z.ecc.android.databinding.FragmentScanBinding
import cash.z.ecc.android.di.viewmodel.activityViewModel
import cash.z.ecc.android.di.viewmodel.viewModel
import cash.z.ecc.android.ext.onClickNavBack
import cash.z.ecc.android.feedback.Report
import cash.z.ecc.android.feedback.Report.Tap.SCAN_BACK
import cash.z.ecc.android.ui.base.BaseFragment
import cash.z.ecc.android.ui.send.SendViewModel
import cash.z.ecc.android.util.twig
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanFragment : BaseFragment<FragmentScanBinding>() {

    override val screen = Report.Screen.SCAN

    private val viewModel: ScanViewModel by viewModel()

    private val sendViewModel: SendViewModel by activityViewModel()

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private var cameraExecutor: ExecutorService? = null

    override fun inflate(inflater: LayoutInflater): FragmentScanBinding =
        FragmentScanBinding.inflate(inflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (cameraExecutor != null) cameraExecutor?.shutdown()
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.backButtonHitArea.onClickNavBack() { tapped(SCAN_BACK) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!allPermissionsGranted()) getRuntimePermissions()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            Runnable {
                bindPreview(cameraProviderFuture.get())
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor?.shutdown()
        cameraExecutor = null
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        // Most of the code here is adapted from: https://github.com/android/camera-samples/blob/master/CameraXBasic/app/src/main/java/com/android/example/cameraxbasic/fragments/CameraFragment.kt
        // it's worth keeping tabs on that implementation because they keep making breaking changes to these APIs!

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { binding.preview.display.getRealMetrics(it) }
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        val rotation = binding.preview.display.rotation

        val preview =
            Preview.Builder().setTargetName("Preview").setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation).build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val imageAnalysis = ImageAnalysis.Builder().setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(
            cameraExecutor!!,
            QrAnalyzer { q, i ->
                onQrScanned(q, i)
            }
        )

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            preview.setSurfaceProvider(binding.preview.surfaceProvider)
        } catch (t: Throwable) {
            // TODO: consider bubbling this up to the user
            mainActivity?.feedback?.report(t)
            twig("Error while opening the camera: $t")
        }
    }

    /**
     * Adapted from: https://github.com/android/camera-samples/blob/master/CameraXBasic/app/src/main/java/com/android/example/cameraxbasic/fragments/CameraFragment.kt#L350
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = kotlin.math.max(width, height).toDouble() / kotlin.math.min(
            width,
            height
        )
        if (kotlin.math.abs(previewRatio - (4.0 / 3.0))
            <= kotlin.math.abs(previewRatio - (16.0 / 9.0))
        ) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun onQrScanned(qrContent: String, image: ImageProxy) {
        resumedScope.launch {
            val parsed = viewModel.parse(qrContent)
            if (parsed == null) {
                val network = viewModel.networkName
                binding.textScanError.text = getString(R.string.scan_invalid_address, network, qrContent)
                image.close()
            } else { /* continue scanning*/
                binding.textScanError.text = ""
                sendViewModel.toAddress = parsed
                mainActivity?.safeNavigate(R.id.action_nav_scan_to_nav_send)
            }
        }
    }

//    private fun updateOverlay(detectedObjects: DetectedObjects) {
//        if (detectedObjects.objects.isEmpty()) {
//            return
//        }
//
//        overlay.setSize(detectedObjects.imageWidth, detectedObjects.imageHeight)
//        val list = mutableListOf<BoxData>()
//        for (obj in detectedObjects.objects) {
//            val box = obj.boundingBox
//            val name = "${categoryNames[obj.classificationCategory]}"
//            val confidence =
//                if (obj.classificationCategory != FirebaseVisionObject.CATEGORY_UNKNOWN) {
//                    val confidence: Int = obj.classificationConfidence!!.times(100).toInt()
//                    "$confidence%"
//                } else {
//                    ""
//                }
//            list.add(BoxData("$name $confidence", box))
//        }
//        overlay.set(list)
//    }

    //
    // Permissions
    //

    private val requiredPermissions: Array<String?>
        get() {
            return try {
                val info = mainActivity?.packageManager
                    ?.getPackageInfo(mainActivity?.packageName ?: "", PackageManager.GET_PERMISSIONS)
                val ps = info?.requestedPermissions
                if (ps != null && ps.isNotEmpty()) {
                    ps
                } else {
                    arrayOfNulls(0)
                }
            } catch (e: Exception) {
                arrayOfNulls(0)
            }
        }

    private fun allPermissionsGranted(): Boolean {
        for (permission in requiredPermissions) {
            if (!isPermissionGranted(mainActivity!!, permission!!)) {
                return false
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val allNeededPermissions = arrayListOf<String>()
        for (permission in requiredPermissions) {
            if (!isPermissionGranted(mainActivity!!, permission!!)) {
                allNeededPermissions.add(permission)
            }
        }

        if (allNeededPermissions.isNotEmpty()) {
            requestPermissions(allNeededPermissions.toTypedArray(), CAMERA_PERMISSION_REQUEST)
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 1002

        private fun isPermissionGranted(context: Context, permission: String): Boolean {
            return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
}
