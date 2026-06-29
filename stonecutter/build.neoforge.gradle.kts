import org.gradle.api.tasks.SourceSetContainer
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
	id("mod-platform")
	alias(libs.plugins.neoforge.moddev)
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
	loader = "neoforge"
	dependencies {
		required("minecraft") {
			fabricLikeVersionRange = prop("deps.minecraft")
		}
	}
}

val mainSourceSet = sourceSets["main"]

neoForge {
	version = prop("deps.neoforge")

	val atFile = file("src/main/resources/META-INF/accesstransformer.cfg")
	if (atFile.exists()) {
		accessTransformers.from(atFile)
	}

	runs {
		create("client") {
			client()
		}
		create("server") {
			server()
		}
	}

	mods {
		create("lomka") {
			sourceSet(mainSourceSet)
		}
	}
}

mainSourceSet.java.exclude("lomka/fabric/**")

if (stonecutter.current.parsed < "1.21.9") {
	mainSourceSet.java.exclude(
		"lomka/starl/mixins/com/mojang/blaze3d/systems/MixinRenderSystem.java",
		"lomka/starl/mixins/net/minecraft/client/gui/render/MixinTextureSetup.java",
		"lomka/starl/mixins/com/mojang/blaze3d/platform/MixinTextureUtil.java",
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
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinDynamicUniforms.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinMultiBufferSource.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/texture/MixinTextureAtlas.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/block/model/MixinBakedQuad.java",
		"lomka/starl/mixins/net/minecraft/client/resources/model/MixinQuadCollectionBuilder.java",
		"lomka/starl/mixins/accessor/InvokerQuadCollectionBuilder.java",
		"lomka/starl/mixins/net/minecraft/client/particle/MixinSingleQuadParticle.java",
		"lomka/starl/mixins/net/minecraft/client/model/MixinModel.java",
		"lomka/starl/mixins/net/minecraft/client/model/geom/MixinModelPart.java",
		"lomka/starl/mixins/com/mojang/blaze3d/vertex/MixinPoseStackPose.java",
		"lomka/starl/mixins/com/mojang/blaze3d/vertex/MixinVertexConsumer.java",
		"lomka/starl/mixins/com/mojang/math/MixinQuadrant.java",
		"lomka/starl/utils/QuadrantRotationCache.java"
	)
}

if (stonecutter.current.parsed < "1.21.11" && stonecutter.current.parsed >= "1.21.9") {
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

if (stonecutter.current.parsed >= "26.1") {
	mainSourceSet.java.exclude(
		"lomka/starl/mixins/com/mojang/blaze3d/systems/MixinRenderSystem.java",
		"lomka/starl/mixins/com/mojang/blaze3d/vertex/MixinVertexConsumer.java",
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
		"lomka/starl/mixins/com/mojang/blaze3d/vertex/MixinVertexFormat.java",
		"lomka/starl/mixins/com/mojang/blaze3d/vertex/MixinMeshData.java",
		"lomka/starl/mixins/com/mojang/blaze3d/vertex/MixinMeshDataSortState.java",
		"lomka/starl/mixins/com/mojang/blaze3d/vertex/MixinBufferBuilder.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinDynamicUniforms.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/texture/MixinTextureAtlas.java"
	)
}


tasks.withType<JavaCompile>().configureEach {
if (stonecutter.current.parsed >= "26.1") {
		exclude("**/MixinBlockModelLighter.java")
		exclude("**/MixinPoseStackPose.java")
		exclude("**/MixinModelPart.java")
	}
	if (stonecutter.current.parsed >= "26.2") {
		exclude("**/MixinMeshData.java")
		exclude("**/MixinMeshDataSortState.java")
		exclude("**/MixinBufferBuilder.java")
	}
}

tasks.named<ProcessResources>("processResources") {
	exclude("aw/**")
}

repositories {
	mavenCentral()
}

dependencies {
	if (stonecutter.current.parsed < "1.21.11") {
		compileOnly("org.jspecify:jspecify:1.0.0")
	}
}
