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

```groovy
// build.gradle(.kts)
plugins {
  id("com.android.application")
  // or id("com.android.library")
  id("com.dropbox.dropshots") version "0.1.0"
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

## Usage

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
