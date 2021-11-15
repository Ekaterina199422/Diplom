package ru.netology.diplom.fragments

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.diplom.R
import ru.netology.diplom.ViewModel.LoginViewModel
import ru.netology.diplom.databinding.FragmentLogInBinding
import ru.netology.diplom.utils.AndroidUtils

@AndroidEntryPoint
class LoginFragments : Fragment() {

    companion object {
        const val LOGIN_SUCCESSFUL: String = "LOGIN_SUCCESSFUL"
    }

    private lateinit var savedStateHandle: SavedStateHandle


    private val viewModel: LoginViewModel by viewModels(
        ownerProducer = ::requireParentFragment
    )
    private lateinit var binding: FragmentLogInBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentLogInBinding.inflate(
            inflater,
            container,
            false
        )




        savedStateHandle = findNavController().previousBackStackEntry!!.savedStateHandle
        savedStateHandle.set(LOGIN_SUCCESSFUL, false)


        binding.signInButton.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            val login = binding.login.text.toString().trim()
            val password = binding.password.text.toString().trim()
            viewModel.onSignIn(login, password)
        }

        viewModel.isSignedIn.observe(viewLifecycleOwner) { isSignedId ->
            if (isSignedId) {
                binding.progressBar.visibility = View.GONE
                AndroidUtils.hideKeyboard(requireView())
                savedStateHandle.set(LOGIN_SUCCESSFUL, true)
                findNavController().popBackStack()
                viewModel.invalidateSignedInState()
            }
        }

        setOnCreateNewAccountListener()

        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progressBar.isVisible = state.isLoading

            if (state.hasError) {
                val msg = getString(state.errorMessage ?: R.string.common_error_message)
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT)
                    .setAction("Ok", {})
                    .show()
                viewModel.invalidateDataState()
            }
        }

        binding.authLaterButton.setOnClickListener {
            findNavController().popBackStack(R.id.nav_posts_fragment, false)
        }

        return binding.root
    }

    private fun setOnCreateNewAccountListener() {
        val spanActionText = getString(R.string.tv_make_account_span_action_login_fragment)
        val createAccClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                binding.login.text.clear()
                binding.password.text.clear()
                findNavController().navigate(R.id.action_logInFragment_to_registrFragment)
            }
        }
        SpannableString(spanActionText).apply {
            setSpan(
                createAccClickableSpan,
                0,
                spanActionText.lastIndex + 1,
                Spanned.SPAN_INCLUSIVE_INCLUSIVE
            )
            binding.tvCreateNewAccount.text = this
            binding.tvCreateNewAccount.movementMethod = LinkMovementMethod.getInstance()
        }
    }

}