package com.example.workmanager

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
import com.example.workmanager.PhotoCompressionWorker.Companion.KEY_COMPRESSION_THRESHOLD
import com.example.workmanager.PhotoCompressionWorker.Companion.KEY_IMAGE_URI
import com.example.workmanager.PhotoCompressionWorker.Companion.KEY_RESULT_PATH
import com.example.workmanager.ui.theme.WorkManagerTheme

class MainActivity : ComponentActivity() {

    private lateinit var workManager: WorkManager
    private val viewModel by viewModels<PhotoViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workManager = WorkManager.getInstance(applicationContext)
        enableEdgeToEdge()
        setContent {
            WorkManagerTheme {
                val workerResult = viewModel.workId?.let {
                    workManager.getWorkInfoByIdLiveData(it).observeAsState().value
                }

                LaunchedEffect(key1 = workerResult?.outputData) {
                    if (workerResult?.outputData != null) {
                        val filePath = workerResult.outputData.getString(KEY_RESULT_PATH)
                        filePath?.let{
                            viewModel.updateCompressedBitmap(BitmapFactory.decodeFile(it))
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    viewModel.uncompressedUri?.let {
                        Text("Uncompressed image:")
                        AsyncImage(model = it, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.padding(16.dp))
                    viewModel.compressedBitmap?.let {
                        Text("Compressed image:")
                        Image(bitmap = it.asImageBitmap(), contentDescription = null)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        } ?: return
        viewModel.updateUncompressedUri(uri)

        val request = OneTimeWorkRequestBuilder<PhotoCompressionWorker>()
            .setInputData(
                workDataOf(
                    KEY_IMAGE_URI to uri.toString(),
                    KEY_COMPRESSION_THRESHOLD to 1024 * 20L
                )
            )
            .setConstraints(Constraints(requiresStorageNotLow = true))
            .build()
        viewModel.updateWorkId(request.id)
        workManager.enqueue(request)
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WorkManagerTheme {
        Greeting("Android")
    }
}