package ru.netology.diplom.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import ru.netology.diplom.R
import ru.netology.diplom.ViewModel.PageViewModel
import ru.netology.diplom.databinding.DialogFragmentMakeJobBinding
import ru.netology.diplom.dto.Job
import ru.netology.diplom.utils.AndroidUtils
import java.util.*


class MakeJobDialogFragment : DialogFragment() {

    private val viewModel: PageViewModel by viewModels(
        ownerProducer = ::requireParentFragment
    )
    lateinit var binding: DialogFragmentMakeJobBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogFragmentMakeJobBinding.inflate(inflater, container, false)


        binding.tvStart.setOnClickListener {
            onShowDatePicker(isStartDate = true)
        }

        binding.tvFinish.setOnClickListener {
            onShowDatePicker(isStartDate = false)
        }

        binding.buttonCancel.setOnClickListener {
            dismiss()
        }

        binding.buttonProve.setOnClickListener {
            onSaveNewJob()

        }
        return binding.root
    }

    private fun onSaveNewJob() {
        val company = binding.etCompany.text.toString().trim()

        val position = binding.etPosition.text.toString().trim()

        val dateStart = binding.tvStart.text.toString().trim()

        val dateFinished = if (binding.tvFinish.text.isNullOrEmpty()) {
            null
        } else {
            AndroidUtils.formatDateStringToMillis(
                binding.tvFinish.text.toString().trim()
            )
        }

        val link = binding.etLink.text.toString().trim()

        if (company.isEmpty()) {
            showToast(getString(R.string._no_company_make_job_error))
            return
        }
        if (position.isEmpty()) {
            showToast(getString(R.string._no_position_make_job_error))
            return
        }
        if (dateStart.isEmpty()) {
            showToast(getString(R.string._no_start_date_make_job_error))
            return
        }

        viewModel.createNewJob(
            Job(
                name = company,
                position = position,
                start = AndroidUtils.formatDateStringToMillis(dateStart),
                finish = dateFinished,
                link = link
            )
        )
        dismiss()
    }

    private fun onShowDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        DateFragment(calendar) { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)

            if (isStartDate)
                binding.tvStart.text = AndroidUtils.formatDateToDateString(calendar.time)
            else binding.tvFinish.text = AndroidUtils.formatDateToDateString(calendar.time)

        }.show(childFragmentManager, "datePicker")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
