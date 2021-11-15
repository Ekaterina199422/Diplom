package ru.netology.diplom.fragments

import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import ru.netology.diplom.R
import ru.netology.diplom.ViewModel.AuthViewModel
import ru.netology.diplom.ViewModel.PageViewModel
import ru.netology.diplom.ViewModel.PostViewModel
import ru.netology.diplom.adapter.*
import ru.netology.diplom.databinding.FragmentPageBinding
import ru.netology.diplom.dto.Job
import ru.netology.diplom.dto.Post
import ru.netology.diplom.ui.MainActivity
import ru.netology.diplom.utils.loadCircleCrop


@AndroidEntryPoint
class PagerFragment : Fragment() {

    val profileViewModel: PageViewModel by viewModels(
        ownerProducer = { this }
    )
    private val authViewModel: AuthViewModel by viewModels(
        ownerProducer = ::requireParentFragment
    )
    private lateinit var navController: NavController
    private lateinit var postRecyclerView: PostRecyclerView

    @ExperimentalCoroutinesApi
    @ExperimentalPagingApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPageBinding.inflate(inflater, container, false)
        navController = findNavController()

        val postViewModel: PostViewModel by viewModels(
            ownerProducer = ::requireParentFragment
        )

        val navArgs: PagerFragmentArgs by navArgs()
        profileViewModel.setAuthorId(navArgs.authorId)

        profileViewModel.getUserById()



        binding.appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            binding.swipeToRefresh.isEnabled = verticalOffset == 0
        })

        postRecyclerView = binding.rvPosts

        val jobAdapter = JobAdapter(object : OnJobButtonInteractionListener {
            override fun onDeleteJob(job: Job) {
                profileViewModel.deleteJobById(job.id)
            }

            override fun onLinkClicked(url: String) {
                CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                    .launchUrl(requireContext(), Uri.parse(url))
            }

        })

        val postAdapter = PostAdapter(object : OnPostButtonInteractionListener {
            override fun onPostLike(post: Post) {
                profileViewModel.likeWallPostById(post)
            }

            override fun onPostRemove(post: Post) {
                profileViewModel.deletePost(post.id)
            }

            override fun onPostEdit(post: Post) {
                postViewModel.editPost(post)
                navController.navigate(R.id.action_nav_page_fragment_to_makeEditPostFragment)
            }

            override fun onAvatarClicked(post: Post) {
                profileViewModel.getLatestWallPosts()
                profileViewModel.loadJobsFromServer()
            }

            override fun onLinkClicked(url: String) {
                CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .build()
                    .launchUrl(requireContext(), Uri.parse(url))
            }
        })

        val itemAnimator: DefaultItemAnimator = object : DefaultItemAnimator() {
            override fun canReuseUpdatedViewHolder(viewHolder: RecyclerView.ViewHolder): Boolean {
                return true
            }
        }
        binding.rvPosts.itemAnimator = itemAnimator

        binding.rvPosts.adapter = postAdapter
        binding.rvPosts.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            )
        )

        binding.pageToolbarLayout.rvJobs.adapter = jobAdapter

        profileViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            (activity as MainActivity?)?.setActionBarTitle(user.login)

            user.avatar?.let {
                binding.pageToolbarLayout.ivAvatar.loadCircleCrop(it)
            } ?: binding.pageToolbarLayout.ivAvatar.setImageDrawable(
                AppCompatResources.getDrawable(requireContext(), R.drawable.ic_no_avatar_user)
            )

            binding.pageToolbarLayout.tvFirstName.text = user.name
        }

        profileViewModel.profileUserId.observe(viewLifecycleOwner) { authorId ->
            if (authViewModel.isAuthenticated) {
                if (authorId != profileViewModel.myId) {
                    binding.pageToolbarLayout.buttonAddJob.visibility = View.GONE
                } else {
                    setHasOptionsMenu(true)
                }
            }
        }

        profileViewModel.loadJobsFromServer()

        lifecycleScope.launchWhenCreated {
            profileViewModel.getWallPosts().collectLatest {
                postAdapter.submitData(it)
            }
        }
        postAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (positionStart == 0) {
                    binding.rvPosts.smoothScrollToPosition(0)
                }
            }
        })
        profileViewModel.getAllJobs().observe(viewLifecycleOwner) {
            val oldCount = jobAdapter.itemCount
            jobAdapter.submitList(it) {
                if (it.size > oldCount) {
                    binding.pageToolbarLayout.rvJobs.smoothScrollToPosition((0))
                }
            }
            binding.pageToolbarLayout.rvJobs.isVisible = it.isNotEmpty()
        }

        binding.pageToolbarLayout.buttonAddJob.setOnClickListener {
            MakeJobDialogFragment().show(childFragmentManager, "createJob")
        }

        binding.swipeToRefresh.setOnRefreshListener {
            postAdapter.refresh()
            profileViewModel.loadJobsFromServer()
        }

        lifecycleScope.launchWhenCreated {
            postAdapter.loadStateFlow.collectLatest { state ->
                binding.swipeToRefresh.isRefreshing = state.refresh == LoadState.Loading

                if (state.source.refresh is LoadState.NotLoading &&
                    state.append.endOfPaginationReached
                ) {
                    binding.emptyListCase.isVisible = postAdapter.itemCount < 1
                }
            }
        }


        profileViewModel.dataState.observe(viewLifecycleOwner) { state ->
            if (state.hasError) {
                val msg = getString(state.errorMessage ?: R.string.common_error_message)
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT)
                    .setAction(getString(R.string.ok_action), {})
                    .show()
                profileViewModel.invalidateDataState()
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!authViewModel.isAuthenticated)
            navController.navigate(R.id.logInFragment)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_pager_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sign_out -> {
                profileViewModel.onSignOut()
                navController.popBackStack()
                true
            }
            else -> false
        }
    }

    override fun onResume() {
        if (::postRecyclerView.isInitialized) postRecyclerView.createPlayer()
        super.onResume()
    }

    override fun onPause() {
        if (::postRecyclerView.isInitialized) postRecyclerView.releasePlayer()
        super.onPause()
    }


    override fun onStop() {
        if (::postRecyclerView.isInitialized) postRecyclerView.releasePlayer()
        super.onStop()
    }

}