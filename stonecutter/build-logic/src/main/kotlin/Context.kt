import dev.kikugie.stonecutter.StonecutterExperimentalAPI
import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project

@OptIn(StonecutterExperimentalAPI::class)
class Context(
	val project: Project,
	val extension: ModPlatformExtension,
	val loader: Loader,
	val stonecutter: StonecutterBuildExtension
) {
	private fun require(key: String): String =
		runCatching { stonecutter.properties.get<String>(key) }.getOrNull()?.takeIf { it.isNotBlank() }
			?: error("Missing required property '$key' in stonecutter.properties.toml")

	private fun optional(key: String, fallback: String = ""): String =
		runCatching { stonecutter.properties.get<String>(key) }.getOrNull()?.takeIf { it.isNotBlank() } ?: fallback

	val stonecutterVersion: String by lazy { stonecutter.current.version }

	val currentMcVersion: String by lazy {
		optional("deps.minecraft", stonecutter.current.version)
	}

	val minecraftMaxVersion: String by lazy {
		optional("deps.minecraft_max", currentMcVersion)
	}

	val modId: String by lazy { require("mod.id") }
	val modName: String by lazy { require("mod.name") }
	val modGroup: String by lazy { require("mod.group") }
	val modVersion: String by lazy { require("mod.version") }
	val description: String by lazy { optional("mod.description") }

	val sourcesUrl: String by lazy { optional("mod.sources_url", "") }
	val homepageUrl: String by lazy { optional("mod.homepage_url", "") }
	val issuesUrl: String by lazy { optional("mod.issues_url", "$sourcesUrl/issues") }

	val authors: List<String> by lazy {
		runCatching {
			stonecutter.properties.raw("mod", "authors").asList().map { it.toString() }
		}.getOrElse { listOf("Starlev") }
	}

	val baseVersion: String by lazy { "$modVersion" }
	val fullVersion: String by lazy { "$baseVersion-${loader.id}+$stonecutterVersion" }

	val javaVersion: JavaVersion by lazy {
		when {
			stonecutter.eval(currentMcVersion, ">=26") -> JavaVersion.VERSION_25
			stonecutter.eval(currentMcVersion, ">=1.20.6") -> JavaVersion.VERSION_21
			stonecutter.eval(currentMcVersion, ">=1.18") -> JavaVersion.VERSION_17
			else -> JavaVersion.VERSION_1_8
		}
	}
}
