import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinTargetWithNodeJsDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
	kotlin("multiplatform")
	id("org.jmailen.kotlinter")
}

private val libs = the<LibrariesForLibs>() // workaround for https://github.com/gradle/gradle/issues/15383

kotlin {
	jvmToolchain(21)
	explicitApi()

	js {
		browser()
		registerNodeJsTarget()
	}

	jvm {
		testRuns["test"].executionTask.configure {
			useJUnitPlatform()
		}
	}

	registerNativeTargets()

	@OptIn(ExperimentalWasmDsl::class)
	wasmJs {
		browser()
		nodejs()
	}

	@OptIn(ExperimentalWasmDsl::class)
	wasmWasi {
		nodejs()
	}

	sourceSets {
		commonTest.dependencies {
			implementation(kotlin("test-common"))
		}

		jsMain.dependencies {
			implementation(kotlin("stdlib-js"))
		}

		jvmTest.dependencies {
			implementation(kotlin("test-junit5"))
			runtimeOnly(libs.junit)
		}

		jsTest.dependencies {
			implementation(kotlin("test-js"))
		}

		wasmJsTest.dependencies {
			implementation(kotlin("test-wasm-js"))
		}

		wasmWasiTest.dependencies {
			implementation(kotlin("test-wasm-wasi"))
		}
	}

	compilerOptions {
		freeCompilerArgs.add("-Xexpect-actual-classes")
	}
}

// JS task configuration

rootProject.plugins.withType<YarnPlugin> {
	// This isn't really a gradle file, but it's emitted as part of the build process at least
	rootProject.the<YarnRootExtension>().lockFileDirectory = project.rootDir.resolve("gradle/kotlin-js-store")
}

allprojects {
	project.plugins.withType<NodeJsPlugin> {
		project.the<NodeJsEnvSpec>().version = "22.12.0" // latest LTS
	}
}

// JVM task configuration

tasks.withType<Jar> {
	from(rootDir.resolve("LICENSE-APACHE")) {
		into("META-INF")
	}

	from(rootDir.resolve("LICENSE-MIT")) {
		into("META-INF")
	}
}

/**
 * Registers every 64-bit native target. 32-bit targets are intentionally unsupported.
 */
private fun KotlinMultiplatformExtension.registerNativeTargets() {
	// Tier 1 targets (https://kotlinlang.org/docs/native-target-support.html#tier-1):
	macosX64()
	macosArm64()
	iosSimulatorArm64()
	iosX64()
	iosArm64()

	// Tier 2 targets (https://kotlinlang.org/docs/native-target-support.html#tier-2):
	linuxX64()
	linuxArm64()
	watchosSimulatorArm64()
	watchosX64()
	watchosArm64()
	tvosSimulatorArm64()
	tvosX64()
	tvosArm64()

	// Tier 3 targets (https://kotlinlang.org/docs/native-target-support.html#tier-3):
	androidNativeArm64()
	androidNativeX64()
	mingwX64()
	watchosDeviceArm64()
}

private fun KotlinTargetWithNodeJsDsl.registerNodeJsTarget() {
	nodejs {
		testTask {
			useMocha {
				timeout = "10s"
			}
		}
	}
}
