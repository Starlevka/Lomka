plugins {
	id("mod-platform")
	id("net.fabricmc.fabric-loom")
}

stonecutter {
	val (version, loader) = current.project.split('-', limit = 2)
	properties.tags(version, loader)

	replacements.string(current.parsed >= "1.21.11") {
		replace("ResourceLocation", "Identifier")
		replace("location()", "identifier()")
	}
    replacements.string(current.parsed < "1.21.11") {
        replace("net.minecraft.client.renderer.rendertype.RenderType", "net.minecraft.client.renderer.RenderType")
    }
    replacements.string(current.parsed >= "26.1") {
        replace("net.minecraft.client.resources.model.AtlasManager", "net.minecraft.client.resources.model.sprite.AtlasManager")
        replace("net.minecraft.client.resources.model.Material", "net.minecraft.client.resources.model.sprite.Material")
    }
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

if (stonecutter.current.parsed >= "26.1") {
	mainSourceSet.java.exclude(
		"lomka/starl/mixins/com/mojang/blaze3d/systems/MixinRenderSystem.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinGameRenderer.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/block/model/MixinBakedQuad.java",
        "lomka/starl/mixins/com/mojang/blaze3d/vertex/MixinPoseStackPose.java",
		"lomka/starl/mixins/net/minecraft/client/model/geom/MixinModelPart.java",
		"lomka/starl/mixins/net/minecraft/client/MixinCamera.java"
	)
}

if (stonecutter.current.parsed >= "26.2") {
	mainSourceSet.java.exclude(
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinMultiBufferSource.java",
		"lomka/starl/mixins/com/mojang/blaze3d/vertex/MixinVertexFormat.java"
	)
}

tasks.withType<JavaCompile>().configureEach {
	if (stonecutter.current.parsed >= "26.1") {
		exclude("**/MixinPoseStackPose.java")
		exclude("**/MixinModelPart.java")
	}
}

dependencies {
	minecraft("com.mojang:minecraft:${prop("deps.minecraft")}")
	implementation("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
}
