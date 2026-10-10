pluginManagement {
	repositories {
		maven {
			name = "GTNH Maven"
			url = uri("https://nexus.gtnewhorizons.com/repository/public/")
			mavenContent {
				includeGroup("com.gtnewhorizons")
				includeGroup("com.gtnewhorizons.retrofuturagradle")
			}
		}
		gradlePluginPortal()
		maven("https://repo.spongepowered.org/repository/maven-public/")
		mavenCentral()
		mavenLocal()
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "lomka-1.12.2"
