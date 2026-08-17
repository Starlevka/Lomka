plugins {
	`kotlin-dsl`
}

repositories {
	mavenCentral()
	gradlePluginPortal()
	maven("https://maven.fabricmc.net/") { name = "Fabric" }
	maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
	maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
}

dependencies {
	implementation("dev.kikugie:stonecutter:0.9.7")
}
