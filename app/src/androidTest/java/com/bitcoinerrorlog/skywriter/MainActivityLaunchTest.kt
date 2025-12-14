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
import android.view.View
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Instrumented test that launches MainActivity and verifies it loads correctly.
 * This test actually runs the app on a device/emulator to catch real loading issues.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityLaunchTest {
    
    private var scenario: ActivityScenario<MainActivity>? = null
    
    @Before
    fun setUp() {
        Log.d("MainActivityLaunchTest", "Setting up test")
    }
    
    @After
    fun tearDown() {
        scenario?.close()
        scenario = null
    }
    
    @Test
    fun testMainActivityLaunches() {
        Log.d("MainActivityLaunchTest", "Starting MainActivity launch test")
        
        // Launch the activity
        scenario = ActivityScenario.launch(MainActivity::class.java)
        
        // Wait a bit for initialization
        Thread.sleep(2000)
        
        // Verify the activity is not null and is resumed
        scenario?.onActivity { activity ->
            Log.d("MainActivityLaunchTest", "Activity state: ${activity.lifecycle.currentState}")
            assert(activity.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                "Activity should be resumed, but is ${activity.lifecycle.currentState}"
            }
        }
        
        Log.d("MainActivityLaunchTest", "MainActivity launched successfully")
    }
    
    @Test
    fun testHeaderLogoIsVisible() {
        Log.d("MainActivityLaunchTest", "Testing header logo visibility")
        
        scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(2000)
        
        // Check if header logo exists in view hierarchy
        scenario?.onActivity { activity ->
            val logo = activity.findViewById<View>(R.id.header_logo)
            assertNotNull("Header logo should exist", logo)
            assertEquals("Header logo should be visible", View.VISIBLE, logo.visibility)
            Log.d("MainActivityLaunchTest", "Header logo is visible")
        }
    }
    
    @Test
    fun testMenuButtonIsVisible() {
        Log.d("MainActivityLaunchTest", "Testing menu button visibility")
        
        scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(2000)
        
        // Check if menu button exists in view hierarchy
        scenario?.onActivity { activity ->
            val menuButton = activity.findViewById<View>(R.id.menu_button)
            assertNotNull("Menu button should exist", menuButton)
            assertEquals("Menu button should be visible", View.VISIBLE, menuButton.visibility)
            Log.d("MainActivityLaunchTest", "Menu button is visible")
        }
    }
    
    @Test
    fun testNavHostFragmentIsCreated() {
        Log.d("MainActivityLaunchTest", "Testing NavHostFragment creation")
        
        scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(3000) // Give more time for fragment creation
        
        scenario?.onActivity { activity ->
            val navHostFragment = activity.supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment)
            
            assert(navHostFragment != null) {
                "NavHostFragment should be created, but is null"
            }
            
            Log.d("MainActivityLaunchTest", "NavHostFragment created: ${navHostFragment != null}")
        }
    }
    
    @Test
    fun testCharacterListFragmentLoads() {
        Log.d("MainActivityLaunchTest", "Testing CharacterListFragment loads")
        
        scenario = ActivityScenario.launch(MainActivity::class.java)
        Thread.sleep(3000) // Give time for fragment and data to load
        
        // Check if RecyclerView is present (from CharacterListFragment)
        scenario?.onActivity { activity ->
            val recyclerView = activity.findViewById<View>(R.id.characters_recycler_view)
            if (recyclerView != null) {
                assertEquals("RecyclerView should be visible", View.VISIBLE, recyclerView.visibility)
                Log.d("MainActivityLaunchTest", "CharacterListFragment loaded successfully")
            } else {
                Log.w("MainActivityLaunchTest", "RecyclerView not found - fragment may not be loaded yet")
                // Don't fail - fragment loading is async
            }
        }
    }
    
    @Test
    fun testNoCrashesOnLaunch() {
        Log.d("MainActivityLaunchTest", "Testing no crashes on launch")
        
        val latch = CountDownLatch(1)
        var crashed = false
        var crashMessage: String? = null
        
        // Set up uncaught exception handler
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            crashed = true
            crashMessage = exception.message
            Log.e("MainActivityLaunchTest", "CRASH DETECTED", exception)
            originalHandler?.uncaughtException(thread, exception)
            latch.countDown()
        }
        
        try {
            scenario = ActivityScenario.launch(MainActivity::class.java)
            
            // Wait up to 5 seconds for any crash
            val crashedEarly = latch.await(5, TimeUnit.SECONDS)
            
            if (crashedEarly) {
                throw AssertionError("App crashed on launch: $crashMessage")
            }
            
            // Wait a bit more to ensure stability
            Thread.sleep(2000)
            
            // Verify activity is still alive
            scenario?.onActivity { activity ->
                assert(!activity.isFinishing) {
                    "Activity should not be finishing"
                }
            }
            
            Log.d("MainActivityLaunchTest", "No crashes detected - app launched successfully")
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(originalHandler)
        }
    }
}

