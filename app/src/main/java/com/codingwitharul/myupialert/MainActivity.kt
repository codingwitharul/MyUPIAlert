package com.codingwitharul.myupialert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.codingwitharul.myupialert.ui.screens.MainScreen
import com.codingwitharul.myupialert.ui.theme.MyUPIAlertTheme

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        val localizedContext = LocaleHelper.applyLocale(newBase)
        super.attachBaseContext(localizedContext ?: newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MainScreen()
        }
    }
}
