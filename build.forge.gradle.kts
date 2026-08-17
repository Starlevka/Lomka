import org.gradle.language.jvm.tasks.ProcessResources

plugins {
	alias(libs.plugins.neoforge.moddev.legacyforge)
}

lomkaPlatform(Loader.Forge)

val mainSourceSet = sourceSets["main"]
val atFile = lomkaAtFile()

legacyForge {
	version = prop("deps.forge")
	accessTransformers.from(atFile)

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

mainSourceSet.resources.srcDir(atFile.parentFile)

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
