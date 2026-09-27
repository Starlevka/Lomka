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

// The loader only picks the access transformer up from META-INF/accesstransformer.cfg inside
// the jar. atFile itself is generated as at/META-INF/accesstransformer.cfg, so the resources
// root has to be at/ - adding at/META-INF would flatten the file to the jar root, where Forge
// silently ignores it and every widened member stays private at runtime.
mainSourceSet.resources.srcDir(atFile.parentFile.parentFile)

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
	if (sc.current.parsed < "1.19.3") {
		compileOnly("org.joml:joml:1.10.5")
	}
}
