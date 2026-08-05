import org.gradle.api.tasks.SourceSetContainer
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
	id("mod-platform")
	alias(libs.plugins.neoforge.moddev.legacyforge)
}

stonecutter {
	val (version, loader) = current.project.split('-', limit = 2)
	properties.tags(version, loader)
}

platform {
	loader = "forge"
	dependencies {
		required("minecraft") {
			fabricLikeVersionRange = prop("deps.minecraft")
		}
	}
}

val mainSourceSet = sourceSets["main"]

legacyForge {
	version = prop("deps.forge")

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

mixin {
	add(mainSourceSet, "lomka.refmap.json")
	config("lomka.mixins.json")
}

mainSourceSet.java.exclude("lomka/fabric/**")
mainSourceSet.java.exclude("lomka/neoforge/**")

if (stonecutter.current.parsed < "1.20.2") {
	mainSourceSet.java.exclude(
		"lomka/starl/mixins/com/mojang/audio/MixinListener.java",
		"lomka/starl/mixins/com/mojang/blaze3d/vertex/MixinBufferBuilder.java",
		"lomka/starl/mixins/com/mojang/blaze3d/vertex/MixinVertexFormat.java",
		"lomka/starl/mixins/net/minecraft/client/multiplayer/MixinClientCommonPacketListenerImpl.java",
		"lomka/starl/mixins/net/minecraft/client/sounds/MixinJOrbisAudioStream.java",
		"lomka/starl/mixins/net/minecraft/client/sounds/MixinSoundBufferLibrary.java",
        "lomka/starl/mixins/net/minecraft/network/MixinCompressionDecoder.java",
        "lomka/starl/mixins/net/minecraft/util/MixinArrayListDeque.java"
	)
}

if (stonecutter.current.parsed < "1.20.2") {
	afterEvaluate {
		the<org.gradle.api.plugins.JavaPluginExtension>().toolchain {
			languageVersion = JavaLanguageVersion.of(17)
		}
	}
}

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

if (stonecutter.current.parsed < "1.21.11") {
	mainSourceSet.java.exclude(
		"lomka/starl/mixins/com/mojang/math/MixinQuadrant.java",
		"lomka/starl/mixins/net/minecraft/client/resources/model/MixinBuilder.java",
		"lomka/starl/mixins/accessor/InvokerBuilder.java"
	)
}

tasks.named<ProcessResources>("processResources") {
	exclude("aw/**")
}

tasks.withType<Jar>().configureEach {
	manifest {
		attributes["MixinConfigs"] = "lomka.mixins.json"
	}
}

repositories {
	mavenCentral()
	maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
}

dependencies {
	annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
	compileOnly("org.jspecify:jspecify:1.0.0")
}
