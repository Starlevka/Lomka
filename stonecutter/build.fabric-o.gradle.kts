plugins {
	id("mod-platform")
	id("net.fabricmc.fabric-loom-remap")
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

if (stonecutter.current.parsed < "1.21.9") {
	mainSourceSet.java.exclude(
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinDynamicUniformStorage.java",
		"lomka/starl/mixins/com/mojang/blaze3d/buffers/MixinGpuBuffer.java",
		"lomka/starl/mixins/com/mojang/blaze3d/buffers/MixinStd140Builder.java",
		"lomka/starl/mixins/com/mojang/blaze3d/buffers/MixinStd140SizeCalculator.java",
		"lomka/starl/mixins/com/mojang/blaze3d/vertex/MixinMeshDataSortState.java",
		"lomka/starl/mixins/net/minecraft/client/resources/model/MixinMaterial.java",
		"lomka/starl/mixins/net/minecraft/client/animation/MixinKeyframeAnimation.java",
		"lomka/starl/mixins/net/minecraft/client/gui/font/MixinFontSet.java",

		"lomka/starl/mixins/net/minecraft/client/renderer/block/model/MixinFaceBakery.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinGameRenderer.java",
		"lomka/starl/mixins/net/minecraft/client/model/MixinModel.java",

		"lomka/starl/mixins/net/minecraft/client/resources/model/geometry/MixinBakedQuad.java",
		"lomka/starl/mixins/net/minecraft/client/particle/MixinSingleQuadParticle.java",
		"lomka/starl/mixins/net/minecraft/client/model/geom/MixinModelPart.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinLevelRenderer.java",
        "lomka/starl/mixins/com/mojang/blaze3d/vertex/MixinPoseStackPose.java",
		"lomka/starl/utils/QuadrantRotationCache.java"
	)
}

if (stonecutter.current.parsed < "1.21.11") {
	mainSourceSet.java.exclude(
		"lomka/starl/mixins/com/mojang/blaze3d/vertex/MixinVertexConsumer.java",
		"lomka/starl/mixins/com/mojang/math/MixinQuadrant.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinDynamicUniforms.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinMultiBufferSource.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/texture/MixinTextureAtlas.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/block/model/MixinBakedQuad.java",
		"lomka/starl/mixins/net/minecraft/client/resources/model/MixinQuadCollectionBuilder.java",
		"lomka/starl/mixins/accessor/InvokerQuadCollectionBuilder.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/block/model/MixinFaceBakery.java",
		"lomka/starl/utils/QuadrantRotationCache.java"
	)
}

dependencies {
	minecraft("com.mojang:minecraft:${prop("deps.minecraft")}")
	mappings(loom.layered { officialMojangMappings() })
	modImplementation("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
	if (stonecutter.current.parsed < "1.21.11") {
		compileOnly("org.jspecify:jspecify:1.0.0")
	}
}
