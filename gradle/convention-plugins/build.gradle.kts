plugins {
	`kotlin-dsl`
}

dependencies {
	implementation(libs.kotlin.gradle.plugin)
	implementation(libs.kotlinter.gradle.plugin)

	// Hacky workaround for https://github.com/gradle/gradle/issues/15383
	implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
