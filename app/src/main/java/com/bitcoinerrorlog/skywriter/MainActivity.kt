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
    private lateinit var navController: androidx.navigation.NavController
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        nfcManager = NFCManager(this)
        
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val controller = navHostFragment?.navController
        if (controller == null) {
            // Log error and return early if navigation fails
            android.util.Log.e("MainActivity", "Failed to get NavController")
            return
        }
        navController = controller
        
        // Hide action bar - we use custom header instead
        supportActionBar?.hide()
        
        // Set up top-level destinations (no back button on these)
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.characterListFragment)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Setup menu button in header
        setupMenuButton()
        
        // Handle initial intent if app was launched with NFC intent
        handleIntent(intent)
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
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_search -> {
                        // Find CharacterListFragment and trigger search
                        val navHostFragment = supportFragmentManager
                            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                        navHostFragment?.childFragmentManager?.fragments?.firstOrNull()?.let { fragment ->
                            if (fragment is com.bitcoinerrorlog.skywriter.ui.list.CharacterListFragment) {
                                fragment.toggleSearch()
                            } else {
                                // If not on character list, navigate there first
                                navController.navigate(R.id.characterListFragment)
                            }
                        }
                        true
                    }
                    R.id.action_check_tag -> {
                        navController.navigate(R.id.tagCheckFragment)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
    
    private fun handleIntent(intent: Intent?) {
        if (intent != null && (intent.action == android.nfc.NfcAdapter.ACTION_TECH_DISCOVERED || 
            intent.action == android.nfc.NfcAdapter.ACTION_TAG_DISCOVERED)) {
            forwardNfcIntent(intent)
        }
    }
    
    override fun onResume() {
        super.onResume()
        nfcManager.enableForegroundDispatch()
    }
    
    override fun onPause() {
        super.onPause()
        nfcManager.disableForegroundDispatch()
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }
    
    private fun forwardNfcIntent(intent: Intent) {
        // Forward NFC intent to current fragment
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        navHostFragment?.childFragmentManager?.fragments?.firstOrNull()?.let { fragment ->
            if (fragment is OnNfcTagDetectedListener) {
                fragment.onNfcTagDetected(intent)
            }
        }
    }
    
    interface OnNfcTagDetectedListener {
        fun onNfcTagDetected(intent: Intent)
    }
}

