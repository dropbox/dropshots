# 📱 Dropshots

Dropshots is a library and Gradle plugin that makes on-device screenshot testing on Android easy.

Other screenshot testing libraries take screenshots in your instrumentation tests on device, then
download those images to your host machine to compare them to reference images, failing at that
step. This means that your screenshot assertions aren't run as part of the rest of your test suite,
and can't easily be run from within your IDE along with the rest of your tests.

Dropshots makes this process easier by performing your screenshot assertions right in your test,
alongside all of your other tests. It's Gradle plugin ensures that your version controlled
reference images are available on your test device so that your test screenshots are able to be
compared to those reference images right within your test.

## Installation

Apply the plugin in your module's `build.gradle` file.

**Using the plugins DSL:**

```groovy
// build.gradle(.kts)
plugins {
  id("com.android.application")
  // or id("com.android.library")
  id("com.dropbox.dropshots") version "0.4.2"
}
```

Note that the plugin is currently published to Maven Central, so you need to add it to the repositories list in `settings.gradle`.

```groovy
// settings.gradle(.kts)
pluginsManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
  }
}
```

<details>
  <summary>Using legacy plugin application</summary>

```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath "com.dropbox.dropshots:dropshots-gradle-plugin:0.4.2"
  }
}

apply plugin: "com.android.application"
// or apply plugin: "com.android.library"
apply plugin: "com.dropbox.dropshots"
```
</details>

## Usage

Once the Dropshots plugin is added to your project, some new tasks will be created to create,
validate and manager your screenshot reference images. While you can use the tasks directly, they
are also automatically injected into your project's task graph to run as part of your normal testing
workflow.

### Write tests

`Dropshots` screenshot tests are simply standard Android Instrumentation tests which use the runtime
library to compare screenshots with reference images. Simply add the `Dropshots` rule to an
instrumentation test, setup the view you'd like to test, and use the `Dropshots.assertSnapshot`
functions to validate them.

```kotlin
class MyTest {
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(TestActivity::class.java)

    @get:Rule
    val dropshots = Dropshots()

    @Before
    fun setup() {
        // Setup your activity however you like
        activityScenarioRule.scenario.onActivity {
            it.supportFragmentManager.beginTransaction()
                .add(android.R.id.content, ScreenshotTestFragment())
                .commitNow()
        }
    }

    @Test
    fun testMatchesFullScreenshot() {
        activityScenarioRule.scenario.onActivity {
            // Assert full-screen snapshots
            dropshots.assertSnapshot("MatchesFullScreenshot")
        }
    }

    @Test
    fun testMatchesActivityScreenshot() {
        activityScenarioRule.scenario.onActivity {
            // Assert activity snapshots
            dropshots.assertSnapshot(it, "MatchesActivityScreenshot")
        }
    }

    @Test
    fun testMatchesViewScreenshot() {
        activityScenarioRule.scenario.onActivity {
            // or assert view snapshots.
            dropshots.assertSnapshot(
                it.findViewById<View>(android.R.id.content),
                name = "MatchesViewScreenshot",
                path = "views/fullscreen" // optional parameter to set path of stored screenshots
            )
        }
    }
}
```

With this test in place, any time the `connectedAndroidTest` task is run the screenshot of the
Activity or View will be validated against the reference images stored in the repository. If any
screenshots fail to match the reference images (within configurable thresholds), then an image will
be written to the additional test output folder that shows the reference image, the actual image,
and the diff of the two. By default, the test report folder is
`${project.buildDir}/outputs/connected_android_test_additional_output/debugAndroidTest/$device/connected`.

The first time you create a screenshot test, however, there won't be any reference images, so you'll
have to create them...

### Updating reference images

Updating reference screenshots is as simple as running the `updateDropshotsScreenshots` Gradle task.
This makes it easy to update screenshots in a single step, without requiring you to
interact with the emulator or use esoteric `adb` commands.

> **Important**: Ensure that you record screenshots on an emulator that's been configured in the
> same way as the emulators on which you'll validate the screenshots.

```shell
./gradlew :path:to:module:recordDebugAndroidTestScreenshots
```

After running this command, you'll see that all reference screenshots for the module will have been
updated in the `src/androidTest/screenshots` directory. After that, running connected tests,
either from the `gradlew` CLI or directly from the IDE, will validate the screenshots against the
new reference images.

### Custom Validation

By default Dropshots will fail assertions if the supplied ImageComparator returns any pixels that
don't match the reference image. If that is too strict for your use case, then you can supply a
custom `ResultValidator` to specify how comparison results should be validated.

The included `CountValidator` validates comparison results which contain no more than the specified
number of pixel differences. The included `ThresholdValidator` validates comparison results which
contain no more then the specific percentage of pixel differences, based on the entire image size.

## License

    Copyright (c) 2022 Dropbox, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
