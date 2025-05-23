plugins {
	alias(libs.plugins.versions)
}

tasks.dependencyUpdates {
	revision = "release"
	gradleReleaseChannel = "current"
	rejectVersionIf { candidate.version.contains("alpha|beta|rc|cr|m|eap|pr|dev".toRegex(RegexOption.IGNORE_CASE)) }
}
