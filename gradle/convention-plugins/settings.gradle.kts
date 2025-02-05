pluginManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()
	}
}

dependencyResolutionManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()
	}

	versionCatalogs {
		create("libs") {
			from(files("../libs.versions.toml"))
		}
	}
}
