# Change Log

## [Unreleased]
[Unreleased]: https://github.com/dropbox/dropshots/compare/0.4.2...HEAD

New:
- Nothing yet!

Changed:
- Updates Gradle plugin to deprecate the `dropshots.record` property.

Fixed:
- Nothing yet!

### Updated Gradle tasks

This version of Dropshots updates the Gradle plugin to deprecate the use of the
`dropshots.record` property in favor of a new `recordDebugAndroidTestScreenshots` task
to update the local reference images.

With this change the behavior of `Dropshots` has also changed, such that it will **always**
record reference images on the test device so that the local copies can be updated without
the need to recompile the app.

## [0.4.2] = 2024-05-21
[0.4.2]: https://github.com/dropbox/dropshots/releases/tags/0.4.2

* Adds support for full screen snapshots. (#59)

## [0.4.1] = 2023-08-07
[0.4.1]: https://github.com/dropbox/dropshots/releases/tags/0.4.1

* Make name param optional in assertSnapshot's overload for bitmaps (#51)
* Allows screenshots to be saved into folders.

## [0.4.0] = 2022-10-14
[0.4.0]: https://github.com/dropbox/dropshots/releases/tags/0.4.0

* Adds support for Gradle configuration cache (#26)
* Adds `ResultValidator` to customize how screenshots are validated (#27)

## [0.3.0] = 2022-08-01
[0.3.0]: https://github.com/dropbox/dropshots/releases/tags/0.3.0

* Replaces `capitalized` call with backwards compatible variant. (#20)
* Updates behavior to fast fail if reference and source image don't match in size. (#17)
* Makes image comparator customizable. (#16)
* Fixes permission issue on API <= 28. (#15)
* Use `withPlugin` api for more efficient setup. (#11)

## [0.2.0] = 2022-06-10
[0.2.0]: https://github.com/dropbox/dropshots/releases/tags/0.2.0

* Removes plugin ordering requirement.
* Makes filename generator configurable.
* Drops min API to 19.
* Fixes dropshots for API 29+ scoped storage.
* Moves screenshots storage to `src/androidTest/screenshots` directory.
* Fixes support for directly tested library projects.
* Adds file existence checks to pull and clear tasks.

## [0.1.1] = 2022-06-03
[0.1.1]: https://github.com/dropbox/dropshots/releases/tags/v0.1.1

* Fixes some directory names and permission issues.

## [0.1.0] - 2022-06-03
[0.1.0]: https://github.com/dropbox/dropshots/releases/tag/v0.1.0

* Initial public release!
