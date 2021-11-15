package ru.netology.diplom.fragments

import android.app.Activity
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toFile
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.snackbar.Snackbar
import ru.netology.diplom.R
import ru.netology.diplom.ViewModel.LoginViewModel
import ru.netology.diplom.databinding.FragmentRegistrBinding
import ru.netology.diplom.utils.AndroidUtils

class RegisterFragment : Fragment() {

    private lateinit var navController: NavController
    private lateinit var binding: FragmentRegistrBinding
    private val viewModel: LoginViewModel by viewModels(
        ownerProducer = ::requireParentFragment
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        navController = findNavController()
        binding = FragmentRegistrBinding.inflate(inflater, container, false)


        viewModel.isSignedIn.observe(viewLifecycleOwner) { isSignedId ->
            if (isSignedId) {
                binding.progressBar.visibility = View.GONE
                AndroidUtils.hideKeyboard(requireView())
                findNavController().popBackStack()
                viewModel.invalidateSignedInState()
            }
        }

        setOnUseExistingAccountListener()

        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progressBar.isVisible = state.isLoading

            if (state.hasError) {
                val msg = getString(state.errorMessage ?: R.string.common_error_message)
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT)
                    .setAction(getString(R.string.ok_action), {})
                    .show()
                viewModel.invalidateDataState()
            }
        }

        binding.signupButton.setOnClickListener {
            val login = binding.userLogin.text.toString().trim()
            val userName = binding.userName.text.toString().trim()
            val password = binding.password.text.toString().trim()
            val passConfirmation = binding.provePassword.text.toString().trim()

            if (userName.isEmpty()) {
                binding.userName.error =
                    getString(R.string.empty_field_error_registr_fragment)
                binding.userName.requestFocus()
                return@setOnClickListener
            }
            if (login.isEmpty()) {
                binding.userLogin.error =
                    getString(R.string.empty_field_error_registr_fragment)
                binding.userLogin.requestFocus()
                return@setOnClickListener
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(login).matches()) {
                binding.userLogin.error = getString(R.string.invalid_email_registr)
                binding.userLogin.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                binding.password.error =
                    (getString(R.string.password_too_short_error_registr_fragment))
                binding.password.requestFocus()
                return@setOnClickListener
            }

            if (password != passConfirmation) {
                binding.provePassword.error =
                    getString(R.string.passwords_not_match_error_registr)
                binding.provePassword.requestFocus()
                return@setOnClickListener
            }
            viewModel.onSignUp(login, password, userName)
            navController.popBackStack()
        }

        val handlePhotoResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
                val resultCode = activityResult.resultCode
                val data = activityResult.data

                if (resultCode == Activity.RESULT_OK) {

                    val fileUri = data?.data!!
                    viewModel.changePhoto(fileUri, fileUri.toFile())
                } else if (resultCode == ImagePicker.RESULT_ERROR) {
                    Snackbar.make(
                        binding.root,
                        ImagePicker.getError(activityResult.data),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }

        binding.cvAvatarCase.setOnClickListener {
            ImagePicker.with(this)
                .cropSquare()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .galleryOnly()
                .galleryMimeTypes(arrayOf("image/png", "image/jpeg"))
                .createIntent { intent ->
                    handlePhotoResult.launch(intent)
                }
        }

        viewModel.photo.observe(viewLifecycleOwner) { photoModel ->
            if (photoModel.uri == null) {
                binding.ivSetAvatar.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_no_avatar_user,
                        null
                    )
                )
                return@observe
            }

            binding.ivSetAvatar.setImageURI(photoModel.uri)


        }

        return binding.root
    }

    private fun setOnUseExistingAccountListener() {
        val spanActionText = getString(R.string.tv_signIn_span_action_registrfragment)
        val createAccClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {

                navController.navigate(R.id.action_registrFragment_to_logInFragment)
            }
        }
        SpannableString(spanActionText).apply {
            setSpan(
                createAccClickableSpan,
                0,
                spanActionText.lastIndex + 1,
                Spanned.SPAN_INCLUSIVE_INCLUSIVE
            )
            binding.tvMakeNewAccount.text = this
            binding.tvMakeNewAccount.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    override fun onDestroyView() {
        viewModel.changePhoto(null, null)
        super.onDestroyView()
    }
}