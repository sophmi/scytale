plugins {
	id("kotlin-conventions")
}

kotlin {
	sourceSets {
		commonMain.dependencies {
			implementation(project(":common"))
		}
	}
}
