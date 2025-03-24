package com.chilisaft.undroaid.ui.serverinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.chilisaft.undroaid.databinding.FragmentServerInfoBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ServerInfoFragment : Fragment() {

    private var _binding: FragmentServerInfoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ServerInfoViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentServerInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.serverInfo.observe(viewLifecycleOwner) { state ->
            when (state) {
                ServerInfoViewModel.ServerInfoState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.textViewError.visibility = View.GONE
                    binding.serverInfoContainer.visibility = View.GONE
                }
                is ServerInfoViewModel.ServerInfoState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.textViewError.visibility = View.GONE
                    binding.serverInfoContainer.visibility = View.VISIBLE
                    val server = state.server
                    binding.textViewName.text = "Name: ${server.name ?: "N/A"}"
                    binding.textViewGuid.text = "GUID: ${server.guid ?: "N/A"}"
                    binding.textViewOwnerUsername.text = "Owner: ${server.owner?.username ?: "N/A"}"
                    binding.textViewRemoteUrl.text = "Remote URL: ${server.remoteUrl ?: "N/A"}"
                    // ... and so on for other fields
                }
                is ServerInfoViewModel.ServerInfoState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.textViewError.visibility = View.VISIBLE
                    binding.textViewError.text = "Error: ${state.message}"
                    binding.serverInfoContainer.visibility = View.GONE
                }
            }
        }

        viewModel.loadServerInformation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Important to avoid memory leaks
    }
}