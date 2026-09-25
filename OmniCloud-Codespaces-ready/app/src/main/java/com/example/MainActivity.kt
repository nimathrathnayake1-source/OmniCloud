package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.OmniCloudApp
import com.example.ui.theme.OmniCloudTheme
import com.example.viewmodel.OmniCloudViewModel

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OmniCloudTheme {
                val viewModel: OmniCloudViewModel = viewModel(
                    factory = OmniCloudViewModel.provideFactory(application)
                )
                OmniCloudApp(viewModel = viewModel)
            }
        }
    }
}
