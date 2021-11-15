package ru.netology.diplom.fragments

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.ExperimentalPagingApi
import ru.netology.diplom.R
import ru.netology.diplom.databinding.FragmentMakeEventBinding
import ru.netology.diplom.dto.Event
import ru.netology.diplom.dto.EventType
import ru.netology.diplom.utils.AndroidUtils
import ru.netology.diplom.utils.loadImage
import ru.netology.diplom.ViewModel.EventViewModel
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.diplom.ui.MainActivity
import java.time.Instant
import java.util.*

@ExperimentalPagingApi
@AndroidEntryPoint
class MakeEventFragment : Fragment() {

    private lateinit var binding: FragmentMakeEventBinding
    private val viewModel: EventViewModel by viewModels(
        ownerProducer = ::requireParentFragment
    )

    private var eventType: EventType? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMakeEventBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)


        viewModel.editedEvent.observe(viewLifecycleOwner) { editedEvent ->
            editedEvent?.let {
                (activity as MainActivity?)
                    ?.setActionBarTitle(getString(R.string.change_event_fragment_title))

                binding.etPostContent.setText(editedEvent.content)
                binding.etPostContent.requestFocus(
                    binding.etPostContent.text.lastIndex
                )

                binding.tvEventDateTime.text =
                    AndroidUtils.formatMillisToDateTimeString(editedEvent.datetime.toEpochMilli())
                AndroidUtils.showKeyboard(binding.etPostContent)

                it.attachment?.let { attachment ->
                    val attachmentUri = attachment.url
                    viewModel.changePhoto(attachmentUri.toUri(), null)
                    binding.ivPhoto.loadImage(attachmentUri)
                    //disable media removal
                    binding.buttonRemovePhoto.visibility = View.GONE
                }

                when (editedEvent.type) {
                    EventType.OFFLINE -> binding.buttonEventTypeGroup.check(R.id.button_type_offline)
                    EventType.ONLINE -> binding.buttonEventTypeGroup.check(R.id.button_type_online)
                }
            }
        }

        binding.groupChooseEventDate.setOnClickListener {
            showDateTimePicker()
        }

        binding.buttonEventTypeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.button_type_online -> eventType = EventType.ONLINE
                    R.id.button_type_offline -> eventType = EventType.OFFLINE
                }
            }
        }

        viewModel.eventDateTime.observe(viewLifecycleOwner) { dateTime ->
            dateTime?.let {
                binding.tvEventDateTime.text = it
            }
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

        binding.buttonPickPhoto.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .provider(ImageProvider.GALLERY)
                .galleryMimeTypes(arrayOf("image/png", "image/jpeg"))
                .createIntent { intent ->
                    handlePhotoResult.launch(intent)
                }
        }

        binding.buttonTakePhoto.setOnClickListener {
            ImagePicker.with(this)
                .crop()
                .compress(1024)
                .maxResultSize(1080, 1080)
                .provider(ImageProvider.CAMERA)
                .createIntent { intent ->
                    handlePhotoResult.launch(intent)
                }
        }


        binding.buttonRemovePhoto.setOnClickListener {
            viewModel.changePhoto(null, null)
        }


        viewModel.photo.observe(viewLifecycleOwner) { photoModel ->
            if (photoModel.uri == null) {
                binding.layoutPhotoCase.visibility = View.GONE
                return@observe
            }

            binding.layoutPhotoCase.visibility = View.VISIBLE
            binding.ivPhoto.setImageURI(photoModel.uri)
        }

        return binding.root
    }

    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()

        DateFragment(calendar) { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)


            TimeFragment(calendar) { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)

                viewModel.setEventDateTime(
                    AndroidUtils.formatDateToDateTimeString(calendar.time)
                )
            }.show(childFragmentManager, "timePicker")
        }.show(childFragmentManager, "datePicker")
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_make_edite_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                if (binding.etPostContent.text.isNullOrEmpty()) {
                    binding.etPostContent.error = getString(R.string.empty_field_error)
                    return false
                }

                if (binding.tvEventDateTime.text.isNullOrEmpty()) {
                    binding.tvEventDateTime.error = getString(R.string.empty_field_error)
                    return false
                }

                if (eventType == null) {
                    Snackbar.make(
                        binding.root,
                        "Please, select event type!",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return false
                }

                val content = binding.etPostContent.text.toString()
                val date =
                    AndroidUtils.formatDateTimeStringToMillis(binding.tvEventDateTime.text.toString())
                val eventType = eventType ?: EventType.OFFLINE



                viewModel.editedEvent.value?.let {
                    viewModel.changePhoto(null, null)
                    viewModel.saveEvent(
                        it.copy(
                            content = content,
                            datetime = Instant.ofEpochMilli(date),
                            type = eventType,
                        )
                    )
                } ?: viewModel.saveEvent(
                    Event(
                        content = content,
                        datetime = Instant.ofEpochMilli(date),
                        type = eventType,
                    )
                )
                AndroidUtils.hideKeyboard(requireView())
                findNavController().popBackStack()
                true
            }
            else -> false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    showAlert()
                }
            })
    }

    private fun showAlert() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.discard_changes_dialog_title))
        builder.setMessage(getString(R.string.discard_changes_dialog_body))
        builder.setPositiveButton(
            getString(R.string.action_leave_dialog_fragment),
            DialogInterface.OnClickListener { dialog, which ->
                viewModel.invalidateEditedEvent()
                viewModel.invalidateEventDateTime()
                viewModel.changePhoto(null, null)
                findNavController().navigateUp()
            })
        builder.setNeutralButton(
            getString(R.string.action_cancel_dialog_fragment),
            DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
            })
        val dialog = builder.create()
        dialog.show()
    }
}