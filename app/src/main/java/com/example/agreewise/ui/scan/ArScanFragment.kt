package com.example.agreewise.ui.scan

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
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
import com.google.ar.core.AugmentedImage
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment

class ArScanFragment : Fragment() {

    private var _binding: FragmentArScanBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var arFragment: ArFragment
    private val augmentedImageMap = HashMap<AugmentedImage, AnchorNode>()
    
    companion object {
        private const val TAG = "ArScanFragment"
    }

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

        // --- ADDED: TAP TO FOCUS ---
        val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                configureArSession() // Trigger a re-configuration/re-focus
                return true
            }
        })

        arFragment.arSceneView.scene.setOnTouchListener { _, motionEvent ->
            gestureDetector.onTouchEvent(motionEvent)
        }

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        binding.btnBack.bringToFront()

        startPulsingAnimation()

        arFragment.arSceneView.scene.addOnUpdateListener { frameTime ->
            val frame = arFragment.arSceneView.arFrame ?: return@addOnUpdateListener
            val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)

            for (augmentedImage in updatedAugmentedImages) {
                when (augmentedImage.trackingState) {
                    TrackingState.TRACKING -> {
                        if (!augmentedImageMap.containsKey(augmentedImage)) {
                            addArOverlay(augmentedImage)
                        }
                    }
                    TrackingState.STOPPED -> {
                        augmentedImageMap.remove(augmentedImage)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun configureArSession() {
        val session = arFragment.arSceneView.session ?: return
        val config = session.config
        config.focusMode = Config.FocusMode.AUTO
        session.configure(config)
    }

    private fun addArOverlay(augmentedImage: AugmentedImage) {
        val anchor = augmentedImage.createAnchor(augmentedImage.centerPose)
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(arFragment.arSceneView.scene)
        augmentedImageMap[augmentedImage] = anchorNode

        ViewRenderable.builder()
            .setView(requireContext(), R.layout.ar_contract_label)
            .build()
            .thenAccept { renderable ->
                val node = com.google.ar.sceneform.Node()
                node.renderable = renderable
                node.setParent(anchorNode)
                
                val view = renderable.view
                view.findViewById<TextView>(R.id.ar_text).text = "Analyzing Contract..."
                view.findViewById<TextView>(R.id.ar_risk_status).text = "Scanning Clauses..."
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopArSession()
        _binding = null
    }
    
    private fun startPulsingAnimation() {
        // Pulsing for the reticle
        val reticleScaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.95f, 1.05f)
        val reticleScaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.95f, 1.05f)
        val reticleAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.5f, 0.9f)

        ObjectAnimator.ofPropertyValuesHolder(binding.arReticle, reticleScaleX, reticleScaleY, reticleAlpha).apply {
            duration = 1500
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            start()
        }

        // Subtler pulsing for the status card to draw attention
        val cardAlpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.8f, 1.0f)
        ObjectAnimator.ofPropertyValuesHolder(binding.arStatusCard, cardAlpha).apply {
            duration = 2000
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.REVERSE
            start()
        }
    }

    private fun stopArSession() {
        try {
            // Clear all augmented images and anchors
            augmentedImageMap.values.forEach { anchorNode ->
                anchorNode.setParent(null)
            }
            augmentedImageMap.clear()
            
            // Stop the AR session
            arFragment.arSceneView.session?.let { session ->
                session.close()
            }
            
            // Scene cleanup is handled by clearing parent nodes above
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}