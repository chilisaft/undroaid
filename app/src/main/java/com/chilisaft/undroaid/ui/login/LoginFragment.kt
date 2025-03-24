package com.chilisaft.undroaid.ui.login

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresExtension
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.advice.array.login.LoginViewModel
import com.chilisaft.undroaid.MainActivity
import com.chilisaft.undroaid.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginFragment: Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // cancelNotificationsWorker()

//        val address = storage.address
//        if (address != null) {
//            binding.address.text = address
//        }

        binding.address.setOnTextChangeListener { text ->
            binding.address.error = null
            validate()
        }

        binding.apiToken.setOnTextChangeListener { text ->
            binding.apiToken.error = null
            validate()
        }

        binding.login.setOnClickListener {
            hideKeyboard(requireActivity())

            // Quick fix for copy-paste issues with My Servers URL
            val ipAddress = binding.address.text
                .replace(".unraid.net/login", ".unraid.net")

            val apiToken = binding.apiToken.text
            viewModel.testLogin(ipAddress, apiToken)
        }

        binding.login.setOnLongClickListener {
            copyUserToClipboard()
            true
        }

        viewModel.status.observe(viewLifecycleOwner) {
            when (it) {
                is LoginScreenState.Init -> {
                    binding.progress.visibility = View.GONE
                    binding.login.isEnabled = false
                }

                is LoginScreenState.Loading -> {
                    binding.progress.visibility = View.VISIBLE
                    binding.login.isEnabled = false
                }

                is LoginScreenState.Error -> {
                    binding.progress.visibility = View.GONE
                    binding.login.isEnabled = true
                    Toast.makeText(requireContext(), it.message, Toast.LENGTH_SHORT).show()
                }

                is LoginScreenState.Success -> {
                    showDashboard()
                }
            }
        }

        viewModel.helpMessage.observe(viewLifecycleOwner) { shouldShow ->
            if (shouldShow) {
                AlertDialog.Builder(requireContext())
                    .setCancelable(false)
                    .setTitle("Need help?")
                    .setMessage(
                        "It appears you're having issues logging into your Unraid Server. \nHere are some tips to solve common issues:\n\n" +
                                "- You must use your root account - this will be the same credentials as the WebUI.\n" +
                                "- You must have a password set on your root account for your Unraid server.\n" +
                                "- Ensure your address is correct - try http or https. Unraid.net URLs are supported and recommended.\n" +
                                "- If you use a VPN, try both on and off your VPN.\n" +
                                "\nIf you're still running into issues, please feel free to contact me directly. - Advice"
                    )
                    .setPositiveButton("OK") { _, _ ->
                        // do nothing
                    }
                    .setNegativeButton("CONTACT DEVELOPER") { _, _ ->
                        //requireActivity().openSupportEmail()
                    }
                    .setOnDismissListener {
                        viewModel.reset()
                    }
                    .show()
            }
        }

        viewModel.loginMessage.observe(viewLifecycleOwner) { message ->
            binding.message.isVisible = message.isNotEmpty()
            binding.message.text = message
        }

        binding.address.setOnFocusChangeListener { _, b ->
            if (b) {
                binding.address.isSelected = true
                binding.apiToken.isSelected = false
            } else {
                validateAddress(showError = true)
            }
        }

        binding.apiToken.setOnFocusChangeListener { _, b ->
            if (b) {
                binding.address.isSelected = false
                binding.apiToken.isSelected = true
            } else {
                validateApiToken(showError = true)
            }
        }
    }


//    private fun cancelNotificationsWorker() {
//        val instance = WorkManager.getInstance(requireContext())
//        instance.cancelAllWorkByTag(NotificationWorker.NOTIFICATION_WORK_TAG)
//    }

    private fun validateAddress(showError: Boolean): Boolean {
        val text = binding.address.text

        val isNotBlank = text.isNotBlank()
        val containsWhitespace = text.contains(" ")
        val hasPrefix = text.startsWith("http://") || text.startsWith("https://")

        if (isNotBlank && !hasPrefix && showError) {
            binding.address.error = "Address must start with http:// or https://"
        }

        if (isNotBlank && containsWhitespace) {
            binding.address.error = "Address cannot contain whitespace character"
        }

        return hasPrefix && !containsWhitespace
    }

    private fun validateApiToken(showError: Boolean): Boolean {
        val isValid = binding.apiToken.text.isNotBlank()
        if (!isValid && showError) {
            binding.apiToken.error = "API token must not be empty"
        }

        return isValid
    }

    private fun validate() {
        val isValid = validateAddress(showError = false) && validateApiToken(showError = false)
        binding.login.isEnabled = isValid
    }

    private fun showDashboard() {
        (requireActivity() as MainActivity).onLogin()
    }

    private fun copyUserToClipboard() {
        val uuid = viewModel.uuid.value
        val clipboard: ClipboardManager =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("UserID", uuid)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), "Copied user id!", Toast.LENGTH_SHORT).show()
    }

    private fun hideKeyboard(activity: Activity) {
        val imm: InputMethodManager =
            activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        var view = activity.currentFocus
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = View(activity)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
