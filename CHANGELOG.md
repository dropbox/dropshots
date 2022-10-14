# Change Log

## Version 0.4.0

_2022-10-14__

* Adds support for Gradle configuration cache (#26)
* Adds `ResultValidator` to customize how screenshots are validated (#27)

## Version 0.3.0

_2022-08-01_

* Replaces `capitalized` call with backwards compatible variant. (#20)
* Updates behavior to fast fail if reference and source image don't match in size. (#17)
* Makes image comparator customizable. (#16)
* Fixes permission issue on API <= 28. (#15)
* Use `withPlugin` api for more efficient setup. (#11)

## Version 0.2.0

_2022-06-10_

* Removes plugin ordering requirement.
* Makes filename generator configurable.
* Drops min API to 19.
* Fixes dropshots for API 29+ scoped storage.
* Moves screenshots storage to `src/androidTest/screenshots` directory.
* Fixes support for directly tested library projects.
* Adds file existence checks to pull and clear tasks.

## Version 0.1.1

_2022-06-03_

* Fixes some directory names and permission issues.

## Version 0.1.0

_2022-06-03_

* Initial public release!
