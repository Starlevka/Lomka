pluginManagement {
	repositories {
		maven {
			// RetroFuturaGradle
			name = "GTNH Maven"
			url = uri("https://nexus.gtnewhorizons.com/repository/public/")
			mavenContent {
				includeGroup("com.gtnewhorizons")
				includeGroup("com.gtnewhorizons.retrofuturagradle")
			}
		}
		gradlePluginPortal()
		// mixingradle is published here, not to the plugin portal. Unused at the moment - see
		// build.gradle.kts on why it must not be applied - but it belongs to this tree.
		maven("https://repo.spongepowered.org/repository/maven-public/")
		mavenCentral()
		mavenLocal()
	}
}

plugins {
	// Supplies the Java 8 compile toolchain. The root build already relies on this; without it
	// there is no JDK 8 on this machine and legacy Forge cannot be compiled at all.
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "lomka-legacy"
