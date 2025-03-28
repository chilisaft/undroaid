package com.chilisaft.undroaid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.chilisaft.undroaid.ui.login.LoginScreen
import com.chilisaft.undroaid.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UndroaidActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (savedInstanceState == null) {
                        LoginScreen()
                    }
                    else {
                        UndroaidNavGraph()
                    }
                }
            }
        }
//
    }

//    private fun showLogin() {
//        supportFragmentManager
//            .beginTransaction()
//            .replace(binding.mainContainer.id, LoginFragment(), "login_fragment")
//            .commit()
//    }
//
//    fun onLogin() {
//        supportFragmentManager
//            .beginTransaction()
//            .replace(
//                binding.mainContainer.id,
//                ServerInfoFragment(),
//                "server_info_fragment"
//            )
//            .commit()
//    }
}