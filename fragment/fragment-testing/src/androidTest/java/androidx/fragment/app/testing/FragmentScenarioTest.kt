/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.fragment.app.testing

import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.FragmentFactory
import androidx.fragment.testing.test.R.id.view_tag_id
import androidx.fragment.testing.test.R.style.ThemedFragmentTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.LifecycleEventObserver
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

private class NoDefaultConstructorFragmentFactory(val arg: String) : FragmentFactory() {
    override fun instantiate(
        classLoader: ClassLoader,
        className: String
    ) = when (className) {
        NoDefaultConstructorFragment::class.java.name -> NoDefaultConstructorFragment(arg)
        else -> super.instantiate(classLoader, className)
    }
}

/**
 * Tests for FragmentScenario's implementation.
 * Verifies FragmentScenario API works consistently across different Android framework versions.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class FragmentScenarioTest {
    @Test
    fun launchFragment() {
        with(launchFragment<StateRecordingFragment>()) {
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.RESUMED)
                // FragmentScenario#launch doesn't attach view to the hierarchy.
                // To test graphical Fragment, use FragmentScenario#launchInContainer.
                assertThat(fragment.isViewAttachedToWindow).isFalse()
                assertThat(fragment.numberOfRecreations).isEqualTo(0)
            }
        }
    }

    @Test
    fun launchFragmentWithArgs() {
        val args = Bundle().apply { putString("my_arg_is", "androidx") }
        with(launchFragment<StateRecordingFragment>(args)) {
            onFragment { fragment ->
                assertThat(fragment.requireArguments().getString("my_arg_is"))
                    .isEqualTo("androidx")
                // FragmentScenario#launch doesn't attach view to the hierarchy.
                // To test graphical Fragment, use FragmentScenario#launchInContainer.
                assertThat(fragment.isViewAttachedToWindow).isFalse()
                assertThat(fragment.numberOfRecreations).isEqualTo(0)
            }
        }
    }

    @Test
    fun launchThemedFragment() {
        with(launchFragment<ThemedFragment>(themeResId = ThemedFragmentTheme)) {
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.RESUMED)
                assertThat(fragment.isViewAttachedToWindow).isFalse()
                assertThat(fragment.hasThemedFragmentTheme()).isTrue()
            }
        }
    }

    @Test
    fun launchFragmentInContainer() {
        with(launchFragmentInContainer<StateRecordingFragment>()) {
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.RESUMED)
                assertThat(fragment.isViewAttachedToWindow).isTrue()
                assertThat(fragment.numberOfRecreations).isEqualTo(0)
            }
        }
    }

    @Test
    fun launchFragmentInContainerWithArgs() {
        val args = Bundle().apply { putString("my_arg_is", "androidx") }
        with(launchFragmentInContainer<StateRecordingFragment>(args)) {
            onFragment { fragment ->
                assertThat(fragment.requireArguments().getString("my_arg_is"))
                    .isEqualTo("androidx")
                assertThat(fragment.isViewAttachedToWindow).isTrue()
                assertThat(fragment.numberOfRecreations).isEqualTo(0)
            }
        }
    }

    @Test
    fun launchThemedFragmentInContainer() {
        with(launchFragmentInContainer<ThemedFragment>(themeResId = ThemedFragmentTheme)) {
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.RESUMED)
                assertThat(fragment.isViewAttachedToWindow).isTrue()
                assertThat(fragment.hasThemedFragmentTheme()).isTrue()
            }
        }
    }

    @Test
    fun launchFragmentWithFragmentFactory() {
        with(
            launchFragment<NoDefaultConstructorFragment>(
                factory = NoDefaultConstructorFragmentFactory("my constructor arg")
            )
        ) {
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.RESUMED)
                assertThat(fragment.constructorArg).isEqualTo("my constructor arg")
                assertThat(fragment.isViewAttachedToWindow).isFalse()
            }
        }
    }

    @Test
    fun launchInContainerFragmentWithFragmentFactory() {
        with(
            launchFragmentInContainer<NoDefaultConstructorFragment>(
                factory = NoDefaultConstructorFragmentFactory("my constructor arg")
            )
        ) {
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.RESUMED)
                assertThat(fragment.constructorArg).isEqualTo("my constructor arg")
                assertThat(fragment.isViewAttachedToWindow).isTrue()
            }
        }
    }

    @Test
    fun launchWithCrossInlineFactoryFunction() {
        var numberOfInstantiations = 0
        with(launchFragment {
            numberOfInstantiations++
            NoDefaultConstructorFragment("my constructor arg")
        }) {
            assertThat(numberOfInstantiations).isEqualTo(1)
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.RESUMED)
                assertThat(fragment.constructorArg).isEqualTo("my constructor arg")
                assertThat(fragment.isViewAttachedToWindow).isFalse()
            }
        }
    }

    @Test
    fun launchInContainerWithCrossInlineFactoryFunction() {
        var numberOfInstantiations = 0
        with(launchFragmentInContainer {
            numberOfInstantiations++
            NoDefaultConstructorFragment("my constructor arg")
        }) {
            assertThat(numberOfInstantiations).isEqualTo(1)
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.RESUMED)
                assertThat(fragment.constructorArg).isEqualTo("my constructor arg")
                assertThat(fragment.numberOfRecreations).isEqualTo(0)
                assertThat(fragment.isViewAttachedToWindow).isTrue()
            }
        }
    }

    @Test
    fun launchInContainerWithEarlyLifecycleCallbacks() {
        var tagSetBeforeOnStart = false
        with(launchFragmentInContainer {
            StateRecordingFragment().also { fragment ->
                fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                    if (viewLifecycleOwner != null) {
                        fragment.requireView().setTag(view_tag_id, "fakeNavController")
                    }
                }
                fragment.lifecycle.addObserver(LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_START) {
                        tagSetBeforeOnStart =
                            fragment.requireView().getTag(view_tag_id) == "fakeNavController"
                    }
                })
            }
        }) {
            assertThat(tagSetBeforeOnStart).isTrue()
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.RESUMED)
                assertThat(fragment.numberOfRecreations).isEqualTo(0)
                assertThat(fragment.isViewAttachedToWindow).isTrue()
            }
        }
    }

    @Test
    fun fromResumedToCreated() {
        with(launchFragmentInContainer<StateRecordingFragment>()) {
            moveToState(State.CREATED)
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.CREATED)
                assertThat(fragment.numberOfRecreations).isEqualTo(0)
            }
        }
    }

    @SdkSuppress(minSdkVersion = 24) // Moving to STARTED is not supported on pre-N devices.
    @Test
    fun fromResumedToStarted() {
        with(launchFragmentInContainer<StateRecordingFragment>()) {
            moveToState(State.STARTED)
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.STARTED)
                assertThat(fragment.numberOfRecreations).isEqualTo(0)
            }
        }
    }

    @Test
    fun fromResumedToResumed() {
        with(launchFragmentInContainer<StateRecordingFragment>()) {
            moveToState(State.RESUMED)
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.RESUMED)
                assertThat(fragment.numberOfRecreations).isEqualTo(0)
            }
        }
    }

    @Test
    fun fromResumedToDestroyed() {
        with(launchFragmentInContainer<StateRecordingFragment>()) {
            moveToState(State.DESTROYED)
        }
    }

    @Test
    fun fromCreatedToCreated() {
        with(launchFragmentInContainer<StateRecordingFragment>()) {
            moveToState(State.CREATED)
            moveToState(State.CREATED)
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.CREATED)
                assertThat(fragment.numberOfRecreations).isEqualTo(0)
            }
        }
    }

    @SdkSuppress(minSdkVersion = 24) // Moving to STARTED is not supported on pre-N devices.
    @Test
    fun fromCreatedToStarted() {
        with(launchFragmentInContainer<StateRecordingFragment>()) {
            moveToState(State.CREATED)
            moveToState(State.STARTED)
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.STARTED)
                assertThat(fragment.numberOfRecreations).isEqualTo(0)
            }
        }
    }

    @Test
    fun fromCreatedToResumed() {
        with(launchFragmentInContainer<StateRecordingFragment>()) {
            moveToState(State.CREATED)
            moveToState(State.RESUMED)
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.RESUMED)
                assertThat(fragment.numberOfRecreations).isEqualTo(0)
            }
        }
    }

    @Test
    fun fromCreatedToDestroyed() {
        with(launchFragmentInContainer<StateRecordingFragment>()) {
            moveToState(State.CREATED)
            moveToState(State.DESTROYED)
        }
    }

    @SdkSuppress(minSdkVersion = 24) // Moving to STARTED is not supported on pre-N devices.
    @Test
    fun fromStartedToCreated() {
        with(launchFragmentInContainer<StateRecordingFragment>()) {
            moveToState(State.STARTED)
            moveToState(State.CREATED)
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.CREATED)
                assertThat(fragment.numberOfRecreations).isEqualTo(0)
            }
        }
    }

    @SdkSuppress(minSdkVersion = 24) // Moving to STARTED is not supported on pre-N devices.
    @Test
    fun fromStartedToStarted() {
        with(launchFragmentInContainer<StateRecordingFragment>()) {
            moveToState(State.STARTED)
            moveToState(State.STARTED)
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.STARTED)
                assertThat(fragment.numberOfRecreations).isEqualTo(0)
            }
        }
    }

    @SdkSuppress(minSdkVersion = 24) // Moving to STARTED is not supported on pre-N devices.
    @Test
    fun fromStartedToResumed() {
        with(launchFragmentInContainer<StateRecordingFragment>()) {
            moveToState(State.STARTED)
            moveToState(State.RESUMED)
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.RESUMED)
                assertThat(fragment.numberOfRecreations).isEqualTo(0)
            }
        }
    }

    @SdkSuppress(minSdkVersion = 24) // Moving to STARTED is not supported on pre-N devices.
    @Test
    fun fromStartedToDestroyed() {
        with(launchFragmentInContainer<StateRecordingFragment>()) {
            moveToState(State.STARTED)
            moveToState(State.DESTROYED)
        }
    }

    @Test
    fun fromDestroyedToDestroyed() {
        with(launchFragmentInContainer<StateRecordingFragment>()) {
            moveToState(State.DESTROYED)
            moveToState(State.DESTROYED)
        }
    }

    @Test
    fun recreateCreatedFragment() {
        with(launchFragmentInContainer<StateRecordingFragment>()) {
            moveToState(State.CREATED)
            recreate()
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.CREATED)
                assertThat(fragment.numberOfRecreations).isEqualTo(1)
            }
        }
    }

    @SdkSuppress(minSdkVersion = 24) // Moving to STARTED is not supported on pre-N devices.
    @Test
    fun recreateStartedFragment() {
        with(launchFragmentInContainer<StateRecordingFragment>()) {
            moveToState(State.STARTED)
            recreate()
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.STARTED)
                assertThat(fragment.numberOfRecreations).isEqualTo(1)
            }
        }
    }

    @Test
    fun recreateResumedFragment() {
        with(launchFragmentInContainer<StateRecordingFragment>()) {
            recreate()
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.RESUMED)
                assertThat(fragment.numberOfRecreations).isEqualTo(1)
            }
        }
    }

    @Test
    fun recreateFragmentWithFragmentFactory() {
        var numberOfInstantiations = 0
        with(launchFragment {
            numberOfInstantiations++
            NoDefaultConstructorFragment("my constructor arg")
        }) {
            assertThat(numberOfInstantiations).isEqualTo(1)
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.RESUMED)
                assertThat(fragment.constructorArg).isEqualTo("my constructor arg")
                assertThat(fragment.numberOfRecreations).isEqualTo(0)
            }
            recreate()
            assertThat(numberOfInstantiations).isEqualTo(2)
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.RESUMED)
                assertThat(fragment.constructorArg).isEqualTo("my constructor arg")
                assertThat(fragment.numberOfRecreations).isEqualTo(1)
            }
        }
    }

    @Test
    fun recreateThemedFragment() {
        with(launchFragmentInContainer<ThemedFragment>(themeResId = ThemedFragmentTheme)) {
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.RESUMED)
                assertThat(fragment.isViewAttachedToWindow).isTrue()
                assertThat(fragment.hasThemedFragmentTheme()).isTrue()
                assertThat(fragment.numberOfRecreations).isEqualTo(0)
            }
            recreate()
            onFragment { fragment ->
                assertThat(fragment.state).isEqualTo(State.RESUMED)
                assertThat(fragment.isViewAttachedToWindow).isTrue()
                assertThat(fragment.hasThemedFragmentTheme()).isTrue()
                assertThat(fragment.numberOfRecreations).isEqualTo(1)
            }
        }
    }

    @Test
    fun fragmentWithOptionsMenu() {
        val uiModeManager = getSystemService(getApplicationContext(), UiModeManager::class.java)!!
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            // Android TV does not support action bar.
            return
        }

        launchFragment<OptionsMenuFragment>().onFragment { fragment ->
            assertThat(fragment.hasOptionsMenu()).isTrue()
        }

        // TODO: Re-enable following checks once openActionBarOverflowOrOptionsMenu() is fixed.
        // https://issuetracker.google.com/issues/69656506
        // openActionBarOverflowOrOptionsMenu(getApplicationContext())
        // onView(withText("Item1")).check(matches(isDisplayed()))
    }
}
