package com.example.agreewise.ui.scan

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.util.Log
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.agreewise.R
import com.example.agreewise.databinding.FragmentArScanBinding
import com.example.agreewise.ml.RiskScanner
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

class ArScanFragment : Fragment() {

    private var _binding: FragmentArScanBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var arFragment: ArFragment
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val isProcessing = AtomicBoolean(false)
    private var lastAnalysisTime = 0L
    private var detectionStartTime = 0L
    private val analysisInterval = 5000L 
    private val autoNavigateDelay = 8000L 
    private var isNavigating = false
    
    private val anchorNodeMap = mutableMapOf<Int, AnchorNode>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arFragment = childFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment
        
        arFragment.arSceneView.viewTreeObserver.addOnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) {
                configureArSession()
            }
        }

        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                showFocusRing(e.x, e.y)
                triggerArFocus()
                return true
            }
        })

        arFragment.arSceneView.scene.setOnTouchListener { _, motionEvent ->
            gestureDetector.onTouchEvent(motionEvent)
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        arFragment.arSceneView.scene.addOnUpdateListener { _ ->
            if (isNavigating) return@addOnUpdateListener
            
            val frame = arFragment.arSceneView.arFrame ?: return@addOnUpdateListener
            
            if (System.currentTimeMillis() - lastAnalysisTime > analysisInterval) {
                processFrame(frame)
            }
            
            if (detectionStartTime != 0L && System.currentTimeMillis() - detectionStartTime > autoNavigateDelay) {
                navigateToResults()
            }
        }
    }

    private fun showFocusRing(x: Float, y: Float) {
        val ring = binding.focusRing
        ring.x = x - (ring.width / 2)
        ring.y = y - (ring.height / 2)
        ring.visibility = View.VISIBLE
        ring.alpha = 1f
        ring.scaleX = 0.5f
        ring.scaleY = 0.5f

        ring.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .alpha(0f)
            .setDuration(400)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    ring.visibility = View.GONE
                }
            })
            .start()
    }

    private fun triggerArFocus() {
        val session = arFragment.arSceneView.session ?: return
        val config = session.config
        config.focusMode = Config.FocusMode.AUTO
        session.configure(config)
    }

    private fun configureArSession() {
        val session = arFragment.arSceneView.session ?: return
        val config = session.config
        config.focusMode = Config.FocusMode.AUTO
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        session.configure(config)
    }

    private fun processFrame(frame: Frame) {
        if (isProcessing.get() || isNavigating) return

        try {
            val image = frame.acquireCameraImage()
            val inputImage = InputImage.fromMediaImage(image, 0)
            
            isProcessing.set(true)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    if (text.isNotBlank() && text.length > 50) {
                        if (detectionStartTime == 0L) detectionStartTime = System.currentTimeMillis()
                        analyzeRisk(text)
                        lastAnalysisTime = System.currentTimeMillis()
                    }
                }
                .addOnCompleteListener {
                    image.close()
                    isProcessing.set(false)
                }
        } catch (e: Exception) {
            Log.e("ArScanFragment", "Error acquiring camera image", e)
        }
    }

    private fun analyzeRisk(text: String) {
        val result = RiskScanner.scanText(text)
        
        activity?.runOnUiThread {
            if (_binding != null) {
                binding.arStatusText.text = getString(R.string.ar_risks_found, result.flaggedKeywords.size)
                updateArOverlays(result.flaggedKeywords, result.score)
            }
        }
    }

    private fun updateArOverlays(risks: List<String>, score: Int) {
        val scene = arFragment.arSceneView.scene
        anchorNodeMap.values.forEach { it.setParent(null) }
        anchorNodeMap.clear()
        
        val layoutId = when {
            score > 60 -> R.layout.ar_label_high 
            score > 30 -> R.layout.ar_label_medium 
            else -> R.layout.ar_label_low 
        }

        val labelText = if (risks.isNotEmpty()) risks[0] else "Standard Clause"

        ViewRenderable.builder()
            .setView(requireContext(), layoutId)
            .build()
            .thenAccept { renderable ->
                val node = com.google.ar.sceneform.Node()
                node.renderable = renderable
                node.localPosition = com.google.ar.sceneform.math.Vector3(0f, 0f, -0.5f)
                
                val anchorNode = AnchorNode()
                anchorNode.setParent(scene)
                node.setParent(anchorNode)
                
                val view = renderable.view
                view.findViewById<TextView>(R.id.risk_description).text = labelText
                
                anchorNodeMap[0] = anchorNode
            }
    }

    private fun navigateToResults() {
        if (isAdded && !isNavigating) {
            isNavigating = true
            findNavController().navigate(R.id.action_ar_scan_to_results)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recognizer.close()
        _binding = null
    }
}