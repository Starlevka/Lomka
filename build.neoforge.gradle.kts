import org.gradle.language.jvm.tasks.ProcessResources

plugins {
	alias(libs.plugins.neoforge.moddev)
}

lomkaPlatform(Loader.NeoForge)

val atFile = lomkaAtFile()

neoForge {
	version = prop("deps.neoforge")
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
			sourceSet(sourceSets["main"])
		}
	}
}

// See build.forge.gradle.kts: the loader reads META-INF/accesstransformer.cfg from the jar, so
// the resources root must be at/ (atFile.parentFile.parentFile), not at/META-INF - otherwise the
// file is packaged at the jar root and NeoForge ignores the whole transformer.
sourceSets["main"].resources.srcDir(atFile.parentFile.parentFile)

tasks.named<ProcessResources>("processResources") {
	exclude("aw/**")
}

repositories {
	mavenCentral()
}

dependencies {
	if (sc.current.parsed < "1.21.11") {
		compileOnly("org.jspecify:jspecify:1.0.0")
	}
}
