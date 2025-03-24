package com.chilisaft.undroaid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.chilisaft.undroaid.databinding.ActivityMainBinding
import com.chilisaft.undroaid.ui.login.LoginFragment
import com.chilisaft.undroaid.ui.serverinfo.ServerInfoFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            showLogin()
        }
    }

    private fun showLogin() {
        supportFragmentManager
            .beginTransaction()
            .replace(binding.mainContainer.id, LoginFragment(), "login_fragment")
            .commit()
    }

    fun onLogin() {
        supportFragmentManager
            .beginTransaction()
            .replace(
                binding.mainContainer.id,
                ServerInfoFragment(),
                "server_info_fragment"
            )
            .commit()
    }
}