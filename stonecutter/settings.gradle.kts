pluginManagement {
	repositories {
		mavenLocal()
		mavenCentral()
		gradlePluginPortal()
		maven("https://maven.fabricmc.net/") { name = "Fabric" }
		maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
		maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
		maven("https://maven.neoforged.net/releases") { name = "NeoForge" }
	}
	includeBuild("build-logic")
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
	id("dev.kikugie.stonecutter") version "0.9.4"
}

stonecutter {
	create(rootProject) {
		fun match(version: String, loader: String) {
			val project = "$version-$loader"
			version(project, version).buildscript = getBuildscript(version, loader)
		}

		match("1.21", "fabric")
		match("1.21", "neoforge")
		match("1.21.9", "fabric")
		match("1.21.9", "neoforge")
		match("1.21.11", "fabric")
		match("1.21.11", "neoforge")
		match("26.1", "fabric")
		match("26.1", "neoforge")
		match("26.2", "fabric")
		match("26.2", "neoforge")

		vcsVersion = "1.21.11-fabric"
	}
}

private fun getBuildscript(version: String, loader: String): String {
	return when {
		loader == "fabric" && version.startsWith("26.") -> "build.fabric-m.gradle.kts"
		loader == "fabric" -> "build.fabric-o.gradle.kts"
		loader == "neoforge" -> "build.neoforge.gradle.kts"
		else -> error("Unknown loader: '$loader'")
	}
}

rootProject.name = "Lomka"
