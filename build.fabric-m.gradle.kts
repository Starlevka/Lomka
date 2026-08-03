plugins {
	id("mod-platform")
	id("net.fabricmc.fabric-loom")
}

stonecutter {
	val (version, loader) = current.project.split('-', limit = 2)
	properties.tags(version, loader)
}

platform {
	loader = "fabric-m"

	dependencies {
		required("minecraft") {
			val mcMin = stonecutter.current.version
			val mcMax = runCatching { prop("deps.minecraft_max") }.getOrNull()
			fabricLikeVersionRange = if (mcMax != null) ">=$mcMin <=$mcMax" else ">=$mcMin"
		}
		required("fabricloader") {
			fabricLikeVersionRange = ">=${prop("deps.fabric-loader")}"
		}
	}
}

loom {
	accessWidenerPath = rootProject.file("versions/${stonecutter.current.project}/src/main/resources/${stonecutter.current.version}.accesswidener")
	runs.named("client") {
		client()
		ideConfigGenerated(true)
		runDir = "run/"
		environment = "client"
		programArgs("--username=Dev")
		configName = "Fabric Client"
	}
	runs.named("server") {
		server()
		ideConfigGenerated(true)
		runDir = "run/"
		environment = "server"
		configName = "Fabric Server"
	}
}

repositories {
	mavenCentral()
}

val mainSourceSet = the<org.gradle.api.plugins.JavaPluginExtension>().sourceSets["main"]

mainSourceSet.java.exclude("lomka/neoforge/**")

// 26.1-26.2+
if (stonecutter.current.version != "1.21.11") {
	mainSourceSet.java.exclude(
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinGameRenderer.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinLightTexture.java"
	)
}

if (stonecutter.current.parsed >= "26.2") {
	mainSourceSet.java.exclude("lomka/starl/mixins/com/mojang/blaze3d/vertex/MixinVertexFormat.java")
}

dependencies {
	minecraft("com.mojang:minecraft:${prop("deps.minecraft")}")
	implementation("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
}
