package ru.netology.diplom.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.netology.diplom.R
import ru.netology.diplom.ViewModel.UsersViewModel
import ru.netology.diplom.adapter.OnUserInteractionListener
import ru.netology.diplom.adapter.UsersAdapter
import ru.netology.diplom.databinding.FragmentUsersBinding
import ru.netology.diplom.dto.User

lateinit var binding: FragmentUsersBinding

@AndroidEntryPoint
class UsersFragment : Fragment() {
    @ExperimentalCoroutinesApi
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUsersBinding.inflate(inflater, container, false)
        val viewModel: UsersViewModel by viewModels()


        val adapter = UsersAdapter(object : OnUserInteractionListener {
            override fun onUserClicked(user: User) {
                val userId = user.id
                val action = UsersFragmentDirections.actionUsersFragmentToNavPageFragment(userId)
                findNavController().navigate(action)
            }
        })

        binding.rvUsers.adapter = adapter

        binding.rvUsers.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )

        binding.swipeToRefresh.setOnRefreshListener {
            viewModel.refreshUsers()
        }

        viewModel.userList.observe(viewLifecycleOwner) { userList ->
            adapter.submitList(userList)

            if (!viewModel.dataState.value?.isRefreshing!! &&
                !viewModel.dataState.value?.isLoading!!
            ) {
                binding.emptyListCase.isVisible = userList.isEmpty()
            }

        }

        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.swipeToRefresh.isRefreshing = state.isRefreshing
            binding.progressBar.isVisible = state.isLoading

            if (state.hasError) {
                val msg = getString(state.errorMessage ?: R.string.common_error_message)
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT)
                    .setAction(getString(R.string.ok_action), {})
                    .show()
                viewModel.invalidateDataState()
            }

        }

        return binding.root
    }
}