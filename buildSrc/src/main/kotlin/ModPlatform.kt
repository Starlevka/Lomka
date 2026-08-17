@file:Suppress("unused", "DuplicatedCode")

import dev.kikugie.stonecutter.StonecutterExperimentalAPI
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

val Project.sc: StonecutterBuildExtension
	get() = extensions.getByType<StonecutterBuildExtension>()

@OptIn(StonecutterExperimentalAPI::class)
fun Project.prop(name: String): String = project.sc.properties.get<String>(name)

abstract class GenerateModManifestTask : DefaultTask() {
	@get:Input
	abstract val content: Property<String>

	@get:OutputFile
	abstract val outputFile: RegularFileProperty

	@TaskAction
	fun generate() {
		val file = outputFile.get().asFile
		file.parentFile.mkdirs()
		file.writeText(content.get())
	}
}

@OptIn(StonecutterExperimentalAPI::class)
fun Project.configureModPlatform(loader: Loader, platform: ModPlatformExtension.() -> Unit = {}) {
	val extension = extensions.create("platform", ModPlatformExtension::class.java).apply {
		this.loader.set(loader.id)
		jarTask.set(loader.jarTask)
		sourcesJarTask.set(loader.sourcesJarTask)
	}.apply(platform)

	val ctx = Context(project = this, extension = extension, loader = loader, stonecutter = sc)

	apply(plugin = "java")
	apply(plugin = "idea")

	version = ctx.fullVersion

	configureProcessResources(ctx)
	configureJava(ctx)
	registerGenerateManifestTask(ctx)
	configureJarTask(ctx)
}

private fun Project.configureJava(ctx: Context) {
	extensions.configure<JavaPluginExtension>("java") {
		withSourcesJar()
		sourceCompatibility = ctx.javaVersion
		targetCompatibility = ctx.javaVersion
	}
}

private fun Project.registerGenerateManifestTask(ctx: Context) {
	val manifestOutputDir = layout.buildDirectory.dir("generated/modManifest")
	val generateTask = tasks.register<GenerateModManifestTask>("generateModManifest") {
		content.set(ctx.loader.generateManifest(ctx))
		outputFile.set(layout.buildDirectory.file("generated/modManifest/${ctx.loader.modManifestPath}"))
	}

	the<JavaPluginExtension>().sourceSets.named("main") { resources.srcDir(manifestOutputDir) }
	tasks.named<ProcessResources>("processResources") { dependsOn(generateTask) }
}

private fun Project.configureProcessResources(ctx: Context) {
	tasks.named<ProcessResources>("processResources") {
		dependsOn(tasks.named("stonecutterGenerate"))
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		filesMatching("*.mixins.json") {
			expand("java" to "JAVA_${ctx.javaVersion.majorVersion}")
		}
		exclude(ctx.loader.excludedResources)
	}
}

private fun Project.configureJarTask(ctx: Context) {
	val generateTask = tasks.named("generateModManifest")
	tasks.withType<Jar>().configureEach {
		archiveBaseName.set(ctx.modId)
		duplicatesStrategy = DuplicatesStrategy.EXCLUDE
		dependsOn(generateTask)
	}
}
