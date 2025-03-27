package com.chilisaft.undroaid

import android.os.Bundle
import android.renderscript.ScriptGroup.Binding
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UndroaidActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UndroaidTheme {
                UndroaidNavGraph()
            }
        }
//        if (savedInstanceState == null) {
//            showLogin()
//        }
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