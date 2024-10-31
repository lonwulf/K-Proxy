package com.lonwulf.kproxy

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import com.lonwulf.kproxy.ui.screens.MainScreen
import com.lonwulf.kproxy.ui.theme.IproxyCloneTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IproxyCloneTheme {
                val snackBarHostState = remember { SnackbarHostState() }

                Scaffold(snackbarHost = { SnackbarHost(snackBarHostState) },
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
//                        if (showAppBars) {
                        BottomNavigation(
//                                navHostController = navController,
//                                currentDestination = currentDestination
                        )
//                        }
                    },
                    topBar = {
                        Column {
//                            if (showAppBars) {
                            Toolbar("")
//                            }
                        }
                    }) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        MainScreen(onConfigurationComplete = {
                            Toast.makeText(this, "Done: ___", Toast.LENGTH_SHORT).show()
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigation() {

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Toolbar(title: String) {
    TopAppBar(
        title = { Text(text = title) }, actions = {},
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            scrolledContainerColor = colorResource(
                id = R.color.white
            ),
            navigationIconContentColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.background,
            actionIconContentColor = colorResource(
                id = R.color.white
            )
        ),
    )
}
