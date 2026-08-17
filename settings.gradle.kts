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
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
	id("dev.kikugie.stonecutter") version "0.9.7"
}

stonecutter {
	create(rootProject) {
		fun buildscript(version: String, loader: String): String = when {
			loader == "fabric" && version >= "26.1" -> "build.fabric-m.gradle.kts"
			loader == "fabric" -> "build.fabric-o.gradle.kts"
			loader == "forge" -> "build.forge.gradle.kts"
			else -> "build.neoforge.gradle.kts"
		}

		fun match(version: String, loader: String) {
			version("$version-$loader", version).buildscript = buildscript(version, loader)
		}

		match("1.20.1", "fabric")
		match("1.20.1", "forge")
		match("1.21", "fabric")
		match("1.21", "neoforge")
		match("1.21.4", "fabric")
		match("1.21.4", "neoforge")
		match("1.21.6", "fabric")
		match("1.21.6", "neoforge")
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

rootProject.name = "Lomka"
