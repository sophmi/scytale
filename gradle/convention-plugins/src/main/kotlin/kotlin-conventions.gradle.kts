import gradle.kotlin.dsl.accessors._70187b50303d45f015b10021a04b2e2f.commonTest
import gradle.kotlin.dsl.accessors._70187b50303d45f015b10021a04b2e2f.sourceSets
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

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
			implementation(kotlin("test"))
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

	@OptIn(ExperimentalKotlinGradlePluginApi::class)
	compilerOptions {
		freeCompilerArgs.add("-Xexpect-actual-classes")
	}
}

// Enables the use of `backtick function names` in common/js/wasm tests - will be stable in Kotlin 2.1
private fun KotlinCompilationTask<*>.allowInvalidJsIdentifiers() {
	compilerOptions.freeCompilerArgs.add("-XXLanguage:+JsAllowInvalidCharsIdentifiersEscaping")
}

private val compileTestKotlinJs: Kotlin2JsCompile by tasks
compileTestKotlinJs.allowInvalidJsIdentifiers()

private val compileTestKotlinWasmJs: KotlinCompilationTask<*> by tasks
compileTestKotlinWasmJs.allowInvalidJsIdentifiers()

rootProject.tasks.withType<KotlinNpmInstallTask> {
	args.add("--ignore-engines")
}

rootProject.the<NodeJsRootExtension>().apply {
	version = "22.6.0"
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
