plugins {
	id("mod-platform")
	id("net.fabricmc.fabric-loom-remap")
}

stonecutter {
	val (version, loader) = current.project.split('-', limit = 2)
	properties.tags(version, loader)
}

platform {
	loader = "fabric-o"

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

configurations.all {
	resolutionStrategy {
		force("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
	}
}

val mainSourceSet = sourceSets["main"]

mainSourceSet.java.exclude("lomka/neoforge/**")
if (stonecutter.current.parsed < "26.1") {
	mainSourceSet.java.exclude(
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinLightmapRenderStateExtractor.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinLightmap.java",
		"lomka/starl/mixins/net/minecraft/util/MixinLightCoordsUtil.java"
	)
}

if (stonecutter.current.parsed < "1.21.6") {
	mainSourceSet.java.exclude(
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinDynamicUniformStorage.java",
		"lomka/starl/mixins/com/mojang/blaze3d/buffers/MixinGpuBuffer.java",
		"lomka/starl/mixins/com/mojang/blaze3d/buffers/MixinStd140Builder.java",
		"lomka/starl/mixins/com/mojang/blaze3d/buffers/MixinStd140SizeCalculator.java",
		"lomka/starl/mixins/com/mojang/blaze3d/vertex/MixinSortState.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinGameRenderer.java",
		"lomka/starl/mixins/net/minecraft/client/model/MixinModel.java",
		"lomka/starl/mixins/net/minecraft/client/model/geom/MixinModelPart.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinItemInHandRenderer.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/texture/MixinTextureAtlas.java"
	)
}

if (stonecutter.current.parsed >= "1.21.4" && stonecutter.current.parsed < "1.21.6") {
	mainSourceSet.java.exclude("lomka/starl/mixins/net/minecraft/client/renderer/MixinLightTexture.java")
}

if (stonecutter.current.parsed >= "1.21.6" && stonecutter.current.parsed < "1.21.9") {
	mainSourceSet.java.exclude(
		"lomka/starl/mixins/com/mojang/blaze3d/vertex/MixinSortState.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinGameRenderer.java",
		"lomka/starl/mixins/net/minecraft/client/model/MixinModel.java",
		"lomka/starl/mixins/net/minecraft/client/model/geom/MixinModelPart.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinItemInHandRenderer.java"
	)
}

if (stonecutter.current.parsed < "1.21.11") {
	mainSourceSet.java.exclude(
		"lomka/starl/mixins/com/mojang/math/MixinQuadrant.java",
		"lomka/starl/mixins/net/minecraft/client/resources/model/MixinBuilder.java",
		"lomka/starl/mixins/accessor/InvokerBuilder.java"
	)
}

if (stonecutter.current.parsed >= "26.2") {
	mainSourceSet.java.exclude("lomka/starl/mixins/com/mojang/blaze3d/vertex/MixinVertexFormat.java")
}

dependencies {
	minecraft("com.mojang:minecraft:${prop("deps.minecraft")}")
	mappings(loom.layered { officialMojangMappings() })
	modImplementation("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
	if (stonecutter.current.parsed < "1.21.11") {
		compileOnly("org.jspecify:jspecify:1.0.0")
	}
}
