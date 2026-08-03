import org.gradle.api.tasks.SourceSetContainer
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
	id("mod-platform")
	alias(libs.plugins.neoforge.moddev)
}

stonecutter {
	val (version, loader) = current.project.split('-', limit = 2)
	properties.tags(version, loader)
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
} else if (stonecutter.current.parsed > "1.21.11") {
	mainSourceSet.java.exclude(
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinGameRenderer.java",
		"lomka/starl/mixins/net/minecraft/client/renderer/MixinLightTexture.java"
	)
}

if (stonecutter.current.parsed >= "26.2") {
	mainSourceSet.java.exclude("lomka/starl/mixins/com/mojang/blaze3d/vertex/MixinVertexFormat.java")
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
