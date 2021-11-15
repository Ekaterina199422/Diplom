package ru.netology.diplom

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.diplom.databinding.ItemLoatStateBinding

class LoadStateAdapter(private val onRetryListener: () -> Unit) : LoadStateAdapter<LoadStateViewHolder>() {
    override fun onBindViewHolder(holder: LoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): LoadStateViewHolder {
        val binding = ItemLoatStateBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return LoadStateViewHolder(binding, onRetryListener )
    }
}

class LoadStateViewHolder(
    private val binding: ItemLoatStateBinding,
    private val onRetryListener: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(loadState: LoadState) {
        binding.apply {
            progressBar.isVisible = loadState is LoadState.Loading
            tvError.isVisible = loadState is LoadState.Error
            buttonRetry.isVisible = loadState is LoadState.Error

            if (loadState is LoadState.Error) {
                tvError.text = loadState.error.localizedMessage
            }

            buttonRetry.setOnClickListener {
                onRetryListener()
            }
        }
    }

}
