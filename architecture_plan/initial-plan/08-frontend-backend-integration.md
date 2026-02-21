# Step 8: Android App — Backend Integration

## Goal

Build a native Android application (Kotlin + Jetpack Compose) that integrates with the Cloud Run backend, implementing camera flow, match confirmation UI, and item display.

## ASCII Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│            Android App (Kotlin + Jetpack Compose)                │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  User Flow:                                                     │
│                                                                  │
│  1. HomeScreen                                                  │
│  ┌──────────────────────────────────────┐                      │
│  │  [Take Photo FAB]                    │                      │
│  │  Shirts: LazyRow of item thumbnails  │                      │
│  │  Pants:  LazyRow of item thumbnails  │                      │
│  │  [View Statistics Button]            │                      │
│  └──────────────────────────────────────┘                      │
│              │                                                   │
│              ▼ (CameraX launched)                               │
│  ┌──────────────────────────────────────┐                      │
│  │  Photo captured → Base64 encoded     │                      │
│  │  → POST /process-outfit              │                      │
│  │  [CircularProgressIndicator]         │                      │
│  └──────────────────────────────────────┘                      │
│              │                                                   │
│              ▼                                                   │
│  2. MatchConfirmationScreen                                     │
│  ┌──────────────────────────────────────┐                      │
│  │  Shirt:                              │                      │
│  │  ┌────────────┐  ┌────────────┐     │                      │
│  │  │ Matched!   │  │ New Item   │     │                      │
│  │  │ 92% match  │  │ [Preview]  │     │                      │
│  │  │ [Confirm]  │  │ [Add New]  │     │                      │
│  │  └────────────┘  └────────────┘     │                      │
│  │                                      │                      │
│  │  Pants:                              │                      │
│  │  ┌────────────┐                      │                      │
│  │  │ Matched!   │                      │                      │
│  │  │ 88% match  │                      │                      │
│  │  │ [Confirm]  │                      │                      │
│  │  └────────────┘                      │                      │
│  └──────────────────────────────────────┘                      │
│              │                                                   │
│              ▼ (Confirm/Add)                                    │
│  ┌──────────────────────────────────────┐                      │
│  │  → POST /confirm-match (matched)     │                      │
│  │  → POST /add-new-item (new)          │                      │
│  └──────────────────────────────────────┘                      │
│              │                                                   │
│              ▼                                                   │
│  3. Back to HomeScreen (updated)                                │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI Framework | Jetpack Compose (Material 3) |
| Navigation | Jetpack Navigation Compose |
| Networking | Retrofit + OkHttp + Moshi |
| Image Loading | Coil (Compose) |
| Camera | CameraX |
| Architecture | MVVM + Repository pattern |
| DI | Hilt |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |

## Data Schemas

See Step 6 response types (ProcessOutfitResponse, ItemMatchResult)

## API Endpoint Signatures

Uses endpoints from Steps 6-7:
- POST /process-outfit
- POST /confirm-match
- POST /add-new-item

## File Structure

```
android/
  app/
    src/main/
      java/com/uniformdist/app/
        MainActivity.kt
        UniformDistApp.kt              # Application class (Hilt)
        di/
          AppModule.kt                 # Hilt dependency injection
          NetworkModule.kt             # Retrofit setup
        data/
          api/
            UniformDistApi.kt          # Retrofit API interface
            ApiConfig.kt               # Base URL configuration
          model/
            ProcessOutfitRequest.kt
            ProcessOutfitResponse.kt
            ConfirmMatchRequest.kt
            AddNewItemRequest.kt
          repository/
            OutfitRepository.kt        # Repository pattern
        ui/
          navigation/
            NavGraph.kt                # Navigation setup
            Screen.kt                  # Screen routes
          theme/
            Theme.kt                   # Material 3 theme
            Color.kt
            Type.kt
          screens/
            home/
              HomeScreen.kt
              HomeViewModel.kt
            camera/
              CameraScreen.kt          # CameraX integration
            confirmation/
              MatchConfirmationScreen.kt
              MatchConfirmationViewModel.kt
          components/
            ItemCard.kt                # Clothing item card
            LoadingOverlay.kt          # Processing indicator
      res/
        values/
          strings.xml
          themes.xml
      AndroidManifest.xml              # Camera permission
    build.gradle.kts                   # App-level dependencies
  build.gradle.kts                     # Project-level config
  settings.gradle.kts
```

## Key Code Snippets

### android/app/src/main/AndroidManifest.xml (permissions)

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-feature android:name="android.hardware.camera" android:required="true" />
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".UniformDistApp"
        android:allowBackup="true"
        android:label="@string/app_name"
        android:theme="@style/Theme.UniformDist">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

### android/app/build.gradle.kts (dependencies)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.uniformdist.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.uniformdist.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0")
    ksp("com.squareup.moshi:moshi-kotlin-codegen:1.15.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // CameraX
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
}
```

### data/api/ApiConfig.kt

```kotlin
package com.uniformdist.app.data.api

object ApiConfig {
    // Replace with your Cloud Run service URL
    const val BASE_URL = "https://your-cloud-run-service-url.run.app"
    const val TIMEOUT_SECONDS = 60L
}
```

### data/api/UniformDistApi.kt

```kotlin
package com.uniformdist.app.data.api

import com.uniformdist.app.data.model.*
import retrofit2.http.Body
import retrofit2.http.POST

interface UniformDistApi {

    @POST("/process-outfit")
    suspend fun processOutfit(
        @Body request: ProcessOutfitRequest
    ): ProcessOutfitResponse

    @POST("/confirm-match")
    suspend fun confirmMatch(
        @Body request: ConfirmMatchRequest
    ): ConfirmMatchResponse

    @POST("/add-new-item")
    suspend fun addNewItem(
        @Body request: AddNewItemRequest
    ): AddNewItemResponse
}
```

### data/model/ProcessOutfitRequest.kt

```kotlin
package com.uniformdist.app.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProcessOutfitRequest(
    val image: String  // base64-encoded JPEG
)

@JsonClass(generateAdapter = true)
data class ItemMatchResult(
    val matched: Boolean,
    val item_id: String? = null,
    val similarity: Double? = null,
    val image_url: String? = null,
    val cropped_url: String? = null,
    val embedding: List<Double>? = null
)

@JsonClass(generateAdapter = true)
data class ProcessOutfitResponse(
    val success: Boolean,
    val shirt: ItemMatchResult? = null,
    val pants: ItemMatchResult? = null,
    val original_photo_url: String? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class ConfirmMatchRequest(
    val item_id: String,
    val item_type: String,
    val original_photo_url: String,
    val similarity_score: Double? = null
)

@JsonClass(generateAdapter = true)
data class ConfirmMatchResponse(
    val success: Boolean,
    val item_id: String,
    val wear_count: Int,
    val last_worn: String? = null
)

@JsonClass(generateAdapter = true)
data class AddNewItemRequest(
    val item_type: String,
    val cropped_image_url: String,
    val embedding: List<Double>,
    val original_photo_url: String,
    val log_wear: Boolean
)

@JsonClass(generateAdapter = true)
data class AddNewItemResponse(
    val success: Boolean,
    val item_id: String
)
```

### di/NetworkModule.kt

```kotlin
package com.uniformdist.app.di

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.uniformdist.app.data.api.ApiConfig
import com.uniformdist.app.data.api.UniformDistApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(ApiConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(ApiConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(ApiConfig.TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit =
        Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

    @Provides
    @Singleton
    fun provideApi(retrofit: Retrofit): UniformDistApi =
        retrofit.create(UniformDistApi::class.java)
}
```

### data/repository/OutfitRepository.kt

```kotlin
package com.uniformdist.app.data.repository

import com.uniformdist.app.data.api.UniformDistApi
import com.uniformdist.app.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutfitRepository @Inject constructor(
    private val api: UniformDistApi
) {
    suspend fun processOutfit(imageBase64: String): ProcessOutfitResponse {
        return api.processOutfit(ProcessOutfitRequest(image = imageBase64))
    }

    suspend fun confirmMatch(
        itemId: String,
        itemType: String,
        originalPhotoUrl: String,
        similarityScore: Double? = null
    ): ConfirmMatchResponse {
        return api.confirmMatch(
            ConfirmMatchRequest(
                item_id = itemId,
                item_type = itemType,
                original_photo_url = originalPhotoUrl,
                similarity_score = similarityScore
            )
        )
    }

    suspend fun addNewItem(
        itemType: String,
        croppedImageUrl: String,
        embedding: List<Double>,
        originalPhotoUrl: String,
        logWear: Boolean
    ): AddNewItemResponse {
        return api.addNewItem(
            AddNewItemRequest(
                item_type = itemType,
                cropped_image_url = croppedImageUrl,
                embedding = embedding,
                original_photo_url = originalPhotoUrl,
                log_wear = logWear
            )
        )
    }
}
```

### ui/navigation/Screen.kt

```kotlin
package com.uniformdist.app.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Camera : Screen("camera")
    data object MatchConfirmation : Screen("match_confirmation")
    data object Statistics : Screen("statistics")
}
```

### ui/navigation/NavGraph.kt

```kotlin
package com.uniformdist.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.uniformdist.app.ui.screens.camera.CameraScreen
import com.uniformdist.app.ui.screens.confirmation.MatchConfirmationScreen
import com.uniformdist.app.ui.screens.home.HomeScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onTakePhoto = { navController.navigate(Screen.Camera.route) },
                onViewStats = { navController.navigate(Screen.Statistics.route) }
            )
        }
        composable(Screen.Camera.route) {
            CameraScreen(
                onPhotoCaptured = { response ->
                    // Pass result via savedStateHandle
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("match_result", response)
                    navController.navigate(Screen.MatchConfirmation.route)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.MatchConfirmation.route) {
            MatchConfirmationScreen(
                onDone = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }
    }
}
```

### ui/screens/home/HomeScreen.kt

```kotlin
package com.uniformdist.app.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTakePhoto: () -> Unit,
    onViewStats: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Uniform Distribution") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onTakePhoto) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Take Photo")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Take a photo of your outfit to log what you're wearing today.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedButton(onClick = onViewStats) {
                Text("View Statistics")
            }
        }
    }
}
```

### ui/screens/camera/CameraScreen.kt

```kotlin
package com.uniformdist.app.ui.screens.camera

import android.Manifest
import android.util.Base64
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.uniformdist.app.data.model.ProcessOutfitResponse
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onPhotoCaptured: (ProcessOutfitResponse) -> Unit,
    onBack: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.result) {
        uiState.result?.let { onPhotoCaptured(it) }
    }

    if (!cameraPermission.status.isGranted) {
        // Permission request UI
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Camera permission is required")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                Text("Grant Permission")
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            // CameraX Preview
            val lifecycleOwner = LocalLifecycleOwner.current
            var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Capture button
            FloatingActionButton(
                onClick = {
                    val capture = imageCapture ?: return@FloatingActionButton
                    val photoFile = File(context.cacheDir, "outfit_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    capture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val bytes = photoFile.readBytes()
                                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                viewModel.processOutfit(base64)
                            }
                            override fun onError(exc: ImageCaptureException) {
                                viewModel.setError("Failed to capture photo: ${exc.message}")
                            }
                        }
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Capture")
            }

            // Loading overlay
            if (uiState.isProcessing) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Card {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Analyzing outfit...")
                            }
                        }
                    }
                }
            }

            // Error snackbar
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(error)
                }
            }
        }
    }
}
```

### ui/screens/confirmation/MatchConfirmationScreen.kt

```kotlin
package com.uniformdist.app.ui.screens.confirmation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.uniformdist.app.data.model.ItemMatchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchConfirmationScreen(
    onDone: () -> Unit,
    viewModel: MatchConfirmationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) onDone()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Confirm Matches") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Shirt section
            uiState.matchResults?.shirt?.let { shirt ->
                ItemMatchSection(
                    itemType = "Shirt",
                    item = shirt,
                    isLoading = uiState.isLoading,
                    onConfirm = { viewModel.confirmMatch("shirt", shirt) },
                    onAddNew = { viewModel.addNewItem("shirt", shirt) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Pants section
            uiState.matchResults?.pants?.let { pants ->
                ItemMatchSection(
                    itemType = "Pants",
                    item = pants,
                    isLoading = uiState.isLoading,
                    onConfirm = { viewModel.confirmMatch("pants", pants) },
                    onAddNew = { viewModel.addNewItem("pants", pants) }
                )
            }
        }
    }
}

@Composable
private fun ItemMatchSection(
    itemType: String,
    item: ItemMatchResult,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onAddNew: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = itemType,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Show the image
            val imageUrl = if (item.matched) item.image_url else item.cropped_url
            imageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "$itemType image",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (item.matched) {
                Text(
                    text = "Match: ${((item.similarity ?: 0.0) * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onConfirm,
                    enabled = !isLoading
                ) {
                    Text("Confirm Match")
                }
            } else {
                Text(
                    text = "New Item Detected",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAddNew,
                    enabled = !isLoading
                ) {
                    Text("Add to Wardrobe")
                }
            }
        }
    }
}
```

### MainActivity.kt

```kotlin
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
```

### UniformDistApp.kt

```kotlin
package com.uniformdist.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class UniformDistApp : Application()
```

## Acceptance Criteria

- [ ] Android project builds and runs on emulator/device
- [ ] CameraX captures photos successfully
- [ ] Photos are Base64 encoded and sent to backend
- [ ] Loading indicator shows during API call
- [ ] Match confirmation screen displays results correctly
- [ ] Matched items show similarity percentage and image
- [ ] New items show cropped preview
- [ ] Confirming match calls backend and returns to home
- [ ] Adding new item creates entry and returns to home
- [ ] Error handling for network failures, timeout, camera errors
- [ ] Images load from signed URLs via Coil
- [ ] Navigation flow works smoothly
- [ ] Camera permission handled gracefully
- [ ] Material 3 theming applied consistently
- [ ] Works on Android 8.0+ (API 26+)

## Setup Instructions

1. **Create Android Project**:
   ```bash
   # Use Android Studio to create a new project:
   # Template: Empty Compose Activity
   # Package: com.uniformdist.app
   # Min SDK: API 26
   ```

2. **Add Dependencies**:
   Copy the dependencies from `build.gradle.kts` above

3. **Configure API URL**:
   Edit `ApiConfig.kt` with your Cloud Run service URL

4. **Build and Run**:
   ```bash
   cd android
   ./gradlew assembleDebug
   # Or use Android Studio Run button
   ```

## Verification

### Test Checklist

- [ ] App installs on Android 8.0+ emulator or device
- [ ] Camera permission dialog appears on first launch
- [ ] Camera preview renders correctly
- [ ] Photo capture works (shutter button)
- [ ] API call completes without errors
- [ ] Loading overlay displays during processing
- [ ] Match results displayed with images
- [ ] Buttons respond to taps
- [ ] Success feedback shown after confirmation
- [ ] Navigation back to home works
- [ ] App handles orientation changes
- [ ] App handles process death/recreation
- [ ] No memory leaks (check with LeakCanary)
