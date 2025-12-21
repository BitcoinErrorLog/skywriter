package com.bitcoinerrorlog.skywriter

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.bitcoinerrorlog.skywriter.nfc.NFCManager

class MainActivity : AppCompatActivity() {
    
    private lateinit var nfcManager: NFCManager
    private var navController: androidx.navigation.NavController? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_main)
            android.util.Log.d("MainActivity", "Layout inflated successfully")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to inflate layout", e)
            throw e
        }
        
        try {
            nfcManager = NFCManager(this)
            android.util.Log.d("MainActivity", "NFCManager created")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to create NFCManager", e)
        }
        
        // Hide action bar - we use custom header instead
        supportActionBar?.hide()
        
        // Setup menu button in header
        setupMenuButton()
        
        // Setup logo click to navigate home
        setupLogoClick()
        
        // Get NavHostFragment - use post to ensure view is ready
        val containerView = findViewById<androidx.fragment.app.FragmentContainerView>(R.id.nav_host_fragment)
        if (containerView != null) {
            containerView.post {
                setupNavigation()
            }
        } else {
            android.util.Log.e("MainActivity", "FragmentContainerView not found!")
        }
        
        // Handle initial intent if app was launched with NFC intent
        handleIntent(intent)
    }
    
    private fun setupNavigation() {
        try {
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            
            if (navHostFragment == null) {
                android.util.Log.e("MainActivity", "NavHostFragment is null")
                return
            }
            
            navController = navHostFragment.navController
            android.util.Log.d("MainActivity", "NavController obtained: ${navController != null}")
            
            // Note: We don't use setupActionBarWithNavController because we hide the action bar
            // Navigation will work without it
            android.util.Log.d("MainActivity", "Navigation setup complete")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error in setupNavigation", e)
        }
    }
    
    /**
     * Gets the NavController, ensuring it's initialized.
     * This is safe to call from menu handlers.
     */
    private fun getNavController(): androidx.navigation.NavController? {
        if (navController != null) {
            return navController
        }
        
        // Try to get it fresh
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        navController = navHostFragment?.navController
        return navController
    }
    
    private fun setupMenuButton() {
        val menuButton = findViewById<android.widget.ImageButton>(R.id.menu_button)
        if (menuButton == null) {
            android.util.Log.w("MainActivity", "Menu button not found")
            return
        }
        menuButton.setOnClickListener { view ->
            val popup = android.widget.PopupMenu(this, view)
            popup.menuInflater.inflate(R.menu.main_menu, popup.menu)
            
            // Show/hide menu items based on current fragment
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
            
            when (currentFragment) {
                is com.bitcoinerrorlog.skywriter.ui.home.HomeFragment -> {
                    // On home screen, hide search and back to home
                    popup.menu.findItem(R.id.action_search)?.isVisible = false
                    popup.menu.findItem(R.id.action_search_amiibo)?.isVisible = false
                    popup.menu.findItem(R.id.action_back_to_home)?.isVisible = false
                }
                is com.bitcoinerrorlog.skywriter.ui.list.CharacterListFragment -> {
                    // On Skylanders list, show Skylanders search, hide Amiibo search
                    popup.menu.findItem(R.id.action_search)?.isVisible = true
                    popup.menu.findItem(R.id.action_search_amiibo)?.isVisible = false
                    popup.menu.findItem(R.id.action_back_to_home)?.isVisible = true
                }
                is com.bitcoinerrorlog.skywriter.ui.amiibo.AmiiboListFragment -> {
                    // On Amiibo list, show Amiibo search, hide Skylanders search
                    popup.menu.findItem(R.id.action_search)?.isVisible = false
                    popup.menu.findItem(R.id.action_search_amiibo)?.isVisible = true
                    popup.menu.findItem(R.id.action_back_to_home)?.isVisible = true
                }
                else -> {
                    // On other screens, show back to home
                    popup.menu.findItem(R.id.action_search)?.isVisible = false
                    popup.menu.findItem(R.id.action_search_amiibo)?.isVisible = false
                    popup.menu.findItem(R.id.action_back_to_home)?.isVisible = true
                }
            }
            
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_search -> {
                        try {
                            val navHostFragment = supportFragmentManager
                                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                            navHostFragment?.childFragmentManager?.fragments?.firstOrNull()?.let { fragment ->
                                if (fragment is com.bitcoinerrorlog.skywriter.ui.list.CharacterListFragment) {
                                    fragment.toggleSearch()
                                } else {
                                    // If not on character list, navigate there first
                                    val controller = getNavController()
                                    controller?.navigate(R.id.characterListFragment)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Error with search", e)
                        }
                        true
                    }
                    R.id.action_search_amiibo -> {
                        try {
                            val navHostFragment = supportFragmentManager
                                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                            navHostFragment?.childFragmentManager?.fragments?.firstOrNull()?.let { fragment ->
                                if (fragment is com.bitcoinerrorlog.skywriter.ui.amiibo.AmiiboListFragment) {
                                    fragment.toggleSearch()
                                } else {
                                    // If not on Amiibo list, navigate there first
                                    val controller = getNavController()
                                    controller?.navigate(R.id.amiiboListFragment)
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Error with Amiibo search", e)
                        }
                        true
                    }
                    R.id.action_check_tag -> {
                        try {
                            val controller = getNavController()
                            if (controller != null) {
                                android.util.Log.d("MainActivity", "Navigating to tag checker")
                                controller.navigate(R.id.tagCheckFragment)
                            } else {
                                android.util.Log.e("MainActivity", "Cannot navigate - NavController is null")
                                android.widget.Toast.makeText(
                                    this,
                                    "Navigation not ready. Please try again.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Error navigating to tag checker", e)
                            android.widget.Toast.makeText(
                                this,
                                "Error: ${e.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        true
                    }
                    R.id.action_back_to_home -> {
                        try {
                            val controller = getNavController()
                            controller?.navigate(R.id.homeFragment)
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Error navigating to home", e)
                        }
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
    
    private fun setupLogoClick() {
        val logo = findViewById<android.widget.ImageView>(R.id.header_logo)
        if (logo == null) {
            android.util.Log.w("MainActivity", "Header logo not found")
            return
        }
        logo.setOnClickListener {
            try {
                val controller = getNavController()
                if (controller != null) {
                    // Navigate to home, clearing the back stack
                    val navOptions = androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.homeFragment, true)
                        .build()
                    controller.navigate(R.id.homeFragment, null, navOptions)
                } else {
                    android.util.Log.e("MainActivity", "Cannot navigate to home - NavController is null")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error navigating to home", e)
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return navController?.navigateUp() ?: super.onSupportNavigateUp()
    }
    
    override fun onResume() {
        super.onResume()
        nfcManager.enableReaderMode { tag ->
            handleTagDetected(tag)
        }
    }

    override fun onPause() {
        super.onPause()
        nfcManager.disableReaderMode()
    }

    private fun handleTagDetected(tag: android.nfc.Tag) {
        // Forward to the current fragment if it implements the listener
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.get(0)
        
        if (currentFragment is OnNfcTagDetectedListener) {
            // Create a fake intent because existing fragments expect it, 
            // but we'll eventually refactor them to take Tag directly
            val intent = Intent().apply {
                putExtra(android.nfc.NfcAdapter.EXTRA_TAG, tag)
            }
            runOnUiThread {
                currentFragment.onNfcTagDetected(intent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // With ReaderMode, onNewIntent is mostly bypassed for NFC, 
        // but we keep it for backward compatibility
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent != null && (intent.action == android.nfc.NfcAdapter.ACTION_TECH_DISCOVERED || 
            intent.action == android.nfc.NfcAdapter.ACTION_TAG_DISCOVERED)) {
            val tag = nfcManager.getTagFromIntent(intent)
            if (tag != null) {
                handleTagDetected(tag)
            }
        }
    }
    
    interface OnNfcTagDetectedListener {
        fun onNfcTagDetected(intent: Intent)
    }
}

