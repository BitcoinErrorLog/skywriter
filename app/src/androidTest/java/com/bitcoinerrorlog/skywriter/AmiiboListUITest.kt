package com.bitcoinerrorlog.skywriter

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import com.bitcoinerrorlog.skywriter.ui.amiibo.AmiiboListFragment
import com.bitcoinerrorlog.skywriter.ui.home.HomeFragment

/**
 * Instrumented test for Amiibo list UI.
 */
@RunWith(AndroidJUnit4::class)
class AmiiboListUITest {
    
    private var scenario: ActivityScenario<MainActivity>? = null
    
    @Before
    fun setUp() {
        Log.d("AmiiboListUITest", "Setting up test")
    }
    
    @After
    fun tearDown() {
        scenario?.close()
        scenario = null
    }
    
    @Test
    fun testHomeFragmentLoads() {
        Log.d("AmiiboListUITest", "Testing HomeFragment loads")
        
        scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(2000) // Wait for activity to initialize
        
        scenario?.onActivity { activity ->
            val navHostFragment = activity.supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            
            assertNotNull("NavHostFragment should exist", navHostFragment)
            
            val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
            assertNotNull("Current fragment should exist", currentFragment)
            
            val fragmentName = currentFragment?.javaClass?.simpleName
            Log.d("AmiiboListUITest", "Current fragment: $fragmentName")
            
            // Should start at home fragment
            assertTrue("Should be HomeFragment, but is $fragmentName", 
                fragmentName == "HomeFragment" || currentFragment is HomeFragment)
            
            Log.d("AmiiboListUITest", "HomeFragment loaded successfully")
        }
    }
    
    @Test
    fun testNavigationToAmiiboList() {
        Log.d("AmiiboListUITest", "Testing navigation to Amiibo list")
        
        scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(2000)
        
        scenario?.onActivity { activity ->
            val navHostFragment = activity.supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            
            val navController = navHostFragment?.navController
            assertNotNull("NavController should exist", navController)
            
            // Navigate to Amiibo list
            try {
                navController?.navigate(R.id.amiiboListFragment)
                Thread.sleep(2000) // Wait for navigation
                
                val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
                assertNotNull("Current fragment should exist", currentFragment)
                
                val fragmentName = currentFragment?.javaClass?.simpleName
                Log.d("AmiiboListUITest", "Current fragment after navigation: $fragmentName")
                
                assertTrue("Should be AmiiboListFragment, but is $fragmentName",
                    fragmentName == "AmiiboListFragment" || currentFragment is AmiiboListFragment)
                
                Log.d("AmiiboListUITest", "Navigation to Amiibo list successful")
            } catch (e: Exception) {
                Log.e("AmiiboListUITest", "Error navigating to Amiibo list", e)
                fail("Navigation failed: ${e.message}")
            }
        }
    }
    
    @Test
    fun testAmiiboListFragmentLoads() {
        Log.d("AmiiboListUITest", "Testing AmiiboListFragment loads")
        
        scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(2000)
        
        scenario?.onActivity { activity ->
            val navHostFragment = activity.supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            
            val navController = navHostFragment?.navController
            navController?.navigate(R.id.amiiboListFragment)
            Thread.sleep(2000)
            
            val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
            assertNotNull("AmiiboListFragment should exist", currentFragment)
            assertTrue("Should be AmiiboListFragment", currentFragment is AmiiboListFragment)
            
            Log.d("AmiiboListUITest", "AmiiboListFragment loaded successfully")
        }
    }
}

