package com.uniformdist.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.uniformdist.app.ui.navigation.NavGraph
import com.uniformdist.app.ui.theme.UniformDistTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UniformDistTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
