import org.gradle.accessors.dm.LibrariesForLibs
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask

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

		jvmTest.dependencies {
			implementation(kotlin("test-junit5"))
			runtimeOnly(libs.junit)
		}

		jsMain.dependencies {
			implementation(kotlin("stdlib-js"))
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

rootProject.the<NodeJsRootExtension>().apply {
	version = "22.12.0"
}

tasks.withType<Jar> {
	from(rootDir.resolve("LICENSE-APACHE")) {
		into("META-INF")
	}

	from(rootDir.resolve("LICENSE-MIT")) {
		into("META-INF")
	}
}

tasks.withType<Test>().configureEach {
	maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}
