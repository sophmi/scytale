import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
	kotlin("multiplatform")
}

private val libs = the<LibrariesForLibs>() // workaround for https://github.com/gradle/gradle/issues/15383

kotlin {
	jvmToolchain(21)
	explicitApi()

	js(IR) {
		browser()
		nodejs()
	}

	jvm {
		testRuns["test"].executionTask.configure {
			useJUnitPlatform()
		}
	}

	@OptIn(ExperimentalWasmDsl::class)
	wasmJs {
		browser()
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
	}

	compilerOptions {
		freeCompilerArgs.add("-Xexpect-actual-classes")
	}
}

tasks.withType<Test>().configureEach {
	maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}

// JS task configuration

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
