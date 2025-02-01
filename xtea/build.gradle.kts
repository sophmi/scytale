plugins {
	id("kotlin-conventions")
}

kotlin {
	jvm {
		compilerOptions {
			freeCompilerArgs.add("-Xadd-modules=jdk.incubator.vector")
		}
	}

	sourceSets {
		commonMain.dependencies {
			implementation(project(":common"))
		}
	}
}

tasks {
	jvmTest {
		jvmArgs = listOf("--add-modules=jdk.incubator.vector")
	}
}
