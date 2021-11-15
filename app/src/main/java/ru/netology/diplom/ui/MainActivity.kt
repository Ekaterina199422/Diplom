package ru.netology.diplom.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.diplom.R
import ru.netology.diplom.ViewModel.AuthViewModel
import ru.netology.diplom.databinding.ActivityMainBinding
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: AuthViewModel by viewModels()


    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    @Inject
    lateinit var firebaseMessaging: FirebaseMessaging

    @Inject
    lateinit var googleApiAvailability: GoogleApiAvailability

    fun setActionBarTitle(title: String) {
        binding.mainToolbar.title = title
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        firebaseMessaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(
                    this.localClassName,
                    "Fetching FCM registration token failed",
                    task.exception
                )
                return@addOnCompleteListener
            }


            val token = task.result
            checkGoogleApiAvailability()
        }



        navController = findNavController(R.id.nav_host_fragment_container)

        val toolbar = binding.mainToolbar
        setSupportActionBar(toolbar)


        val buttonNavView = binding.buttonNavView.apply {
            background = null
            menu.findItem(R.id.empty_item).isEnabled = false
        }


        val topLevelDestinations = setOf(
            R.id.nav_posts_fragment,
            R.id.nav_events_fragment,
            R.id.nav_page_fragment,
            R.id.nav_users_fragment
        )

        appBarConfiguration = AppBarConfiguration.Builder(topLevelDestinations).build()
        NavigationUI.setupActionBarWithNavController(
            this, navController,
            appBarConfiguration
        )



        navController.addOnDestinationChangedListener { _, destination, _ ->

            when (destination.id) {

                R.id.logInFragment -> {
                    toolbar.visibility = View.GONE
                    buttonNavView.visibility = View.GONE
                    binding.buttonAppBar.visibility = View.GONE
                    binding.fabMakeLayout.visibility = View.GONE
                }
                R.id.registrFragment -> {
                    toolbar.visibility = View.GONE
                    binding.buttonAppBar.visibility = View.GONE
                    buttonNavView.visibility = View.GONE
                    binding.fabMakeLayout.visibility = View.GONE
                }
                R.id.makeEditPostFragment -> {
                    buttonNavView.visibility = View.GONE
                    binding.buttonAppBar.visibility = View.GONE
                    binding.fabMakeLayout.visibility = View.GONE
                }
                R.id.makeEventFragment -> {
                    buttonNavView.visibility = View.GONE
                    binding.buttonAppBar.visibility = View.GONE
                    binding.fabMakeLayout.visibility = View.GONE
                }

                else -> {
                    toolbar.visibility = View.VISIBLE
                    binding.buttonAppBar.visibility = View.VISIBLE
                    buttonNavView.visibility = View.VISIBLE
                    binding.fabMakeLayout.visibility = View.VISIBLE
                }

            }
        }

        buttonNavView.setupWithNavController(navController)

        binding.fabAddPost.setOnClickListener {
            navController.navigate(R.id.makeEditPostFragment)
        }

        binding.fabAddEvent.setOnClickListener {
            navController.navigate(R.id.makeEventFragment)
        }


        viewModel.authState.observe(this)
        { user ->

            if (!viewModel.checkIfAskedToLogin && user.id == 0L) {
                navController.navigate(R.id.logInFragment)
                viewModel.setCheckLoginTrue()
            }

            if (user.id == 0L) {
                invalidateOptionsMenu()
                binding.buttonNavView.menu.findItem(R.id.nav_page_fragment).isEnabled = false
                binding.expandableFab.visibility = View.GONE
            } else {
                invalidateOptionsMenu()
                binding.buttonNavView.menu.findItem(R.id.nav_page_fragment).isEnabled = true
                binding.expandableFab.visibility = View.VISIBLE
            }
        }
    }


    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.auth_app_manu, menu)
        menu?.setGroupVisible(R.id.group_sign_in, !viewModel.isAuthenticated)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sign_in -> {
                navController.navigate(R.id.logInFragment)
                true
            }
            else -> false
        }
    }


    private fun checkGoogleApiAvailability() {
        with(googleApiAvailability) {
            val code = isGooglePlayServicesAvailable(this@MainActivity)
            if (code == ConnectionResult.SUCCESS) {
                return@with
            }
            if (isUserResolvableError(code)) {
                getErrorDialog(this@MainActivity, code, 9000).show()
                return
            }
            Toast.makeText(this@MainActivity, R.string.google_play_unavailable, Toast.LENGTH_LONG)
                .show()
            googleApiAvailability.makeGooglePlayServicesAvailable(this@MainActivity)
        }
    }
}