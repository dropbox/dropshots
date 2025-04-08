Releasing
=========

1. Change the version in `gradle.properties` to a non-SNAPSHOT version.
2. Update the `CHANGELOG.md` for the impending release.
   1. Change the `Unreleased` header to the release version.
   2. Add a link URL to ensure the header link works.
   3. Add a new `Unreleased` section to the top.
3. Update the `README.md` for the impending release.
   1. Change the "Installation" section to reflect the new release version.
4. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
5. `git tag -a X.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
6. Update the `gradle.properties` to the next SNAPSHOT version.
7. `git commit -am "Prepare next development version."`
8. `git push && git push --tags` to trigger CI to deploy the release.
