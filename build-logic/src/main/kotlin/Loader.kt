@file:Suppress("unused")

sealed class Loader(val id: String) {
	abstract val jarTask: String
	abstract val sourcesJarTask: String
	abstract val modManifestPath: String
	abstract val excludedResources: List<String>

	abstract fun generateManifest(ctx: Context): String

	sealed class FabricLike(id: String) : Loader(id) {
		override val excludedResources = listOf(
			"META-INF/mods.toml", "META-INF/neoforge.mods.toml", ".cache", "pack.mcmeta"
		)

		override fun generateManifest(ctx: Context): String {
			val contact = mutableMapOf<String, String>()
			if (ctx.sourcesUrl.isNotBlank()) contact["sources"] = ctx.sourcesUrl
			if (ctx.issuesUrl.isNotBlank()) contact["issues"] = ctx.issuesUrl
			if (ctx.homepageUrl.isNotBlank()) contact["homepage"] = ctx.homepageUrl

			val depends = ctx.extension.dependencies.required.associate { it.modid.get() to it.fabricLikeVersionRange.get() }

			return buildString {
				appendLine("{")
				appendLine("  \"schemaVersion\": 1,")
				appendLine("  \"id\": ${jsonStr(ctx.modId)},")
				appendLine("  \"name\": ${jsonStr(ctx.modName)},")
				appendLine("  \"version\": ${jsonStr(ctx.baseVersion)},")
				appendLine("  \"authors\": [${ctx.authors.joinToString(", ") { jsonStr(it) }}],")
				appendLine("  \"contact\": {")
				contact.entries.forEachIndexed { i, (k, v) ->
					val comma = if (i < contact.size - 1) "," else ""
					appendLine("    ${jsonStr(k)}: ${jsonStr(v)}$comma")
				}
				appendLine("  },")
				appendLine("  \"description\": ${jsonStr(ctx.description)},")
				appendLine("  \"icon\": ${jsonStr("assets/${ctx.modId}/icon.png")},")
				appendLine("  \"license\": \"MIT\",")
				appendLine("  \"environment\": \"*\",")
				appendLine("  \"accessWidener\": ${jsonStr("${ctx.stonecutterVersion}.accesswidener")},")
				appendLine("  \"entrypoints\": {")
				appendLine("    \"main\": [\"lomka.fabric.LomkaFabric\"]")
				appendLine("  },")
				appendLine("  \"mixins\": [\"${ctx.modId}.mixins.json\"],")
				appendLine("  \"depends\": {")
				depends.entries.forEachIndexed { i, (k, v) ->
					val comma = if (i < depends.size - 1) "," else ""
					appendLine("    ${jsonStr(k)}: ${jsonStr(v)}$comma")
				}
				appendLine("  }")
				append("}")
			}
		}
	}

	sealed class NeoForgeLike(id: String) : Loader(id) {
		override val jarTask = "jar"
		override val sourcesJarTask = "sourcesJar"
		override val excludedResources = listOf(
			"META-INF/mods.toml", "fabric.mod.json", ".cache", "pack.mcmeta"
		)
	}

	object FabricO : FabricLike("fabric") {
		override val jarTask = "remapJar"
		override val sourcesJarTask = "remapSourcesJar"
		override val modManifestPath = "fabric.mod.json"
	}

	object FabricM : FabricLike("fabric") {
		override val jarTask = "jar"
		override val sourcesJarTask = "sourcesJar"
		override val modManifestPath = "fabric.mod.json"
	}

	object NeoForge : NeoForgeLike("neoforge") {
		override val modManifestPath = "META-INF/neoforge.mods.toml"

		override fun generateManifest(ctx: Context): String {
			val mcVersionRange = if (ctx.stonecutter.eval(ctx.stonecutterVersion, "<=" + ctx.currentMcVersion)) {
				val maxVersion = ctx.minecraftMaxVersion
				if (maxVersion == ctx.currentMcVersion && ctx.stonecutterVersion == ctx.currentMcVersion) {
					"[${ctx.stonecutterVersion},)"
				} else {
					"[${ctx.stonecutterVersion},${maxVersion}]"
				}
			} else {
				"[${ctx.currentMcVersion}]"
			}
			val neoforgeVersionRange = when {
				ctx.currentMcVersion.startsWith("26.") -> "[${ctx.stonecutterVersion},)"
				ctx.stonecutter.eval(ctx.currentMcVersion, "<1.21.10") -> "[21.0,)"
				else -> "[${ctx.currentMcVersion.removePrefix("1.")}-beta,)"
			}

			return buildString {
				appendLine("modLoader = \"javafml\"")
				appendLine("loaderVersion = \"[4,)\"")
				appendLine("license = \"MIT\"")
				appendLine()
				appendLine("[[mods]]")
				appendLine("modId = \"${ctx.modId}\"")
				appendLine("version = \"${ctx.baseVersion}\"")
				appendLine("displayName = \"${ctx.modName}\"")
				appendLine("authors = \"${ctx.authors.firstOrNull() ?: "Starlev"}\"")
				appendLine("description = \"\"\"${ctx.description}\"\"\"")
				appendLine("logoFile = \"assets/${ctx.modId}/icon.png\"")
				appendLine()
				appendLine("[[mixins]]")
				appendLine("config = \"${ctx.modId}.mixins.json\"")
				appendLine()
				appendLine("[[dependencies.${ctx.modId}]]")
				appendLine("modId = \"neoforge\"")
				appendLine("type = \"required\"")
				appendLine("versionRange = \"${neoforgeVersionRange}\"")
				appendLine("ordering = \"NONE\"")
				appendLine()
				appendLine("[[dependencies.${ctx.modId}]]")
				appendLine("modId = \"minecraft\"")
				appendLine("type = \"required\"")
				appendLine("versionRange = \"${mcVersionRange}\"")
				appendLine("ordering = \"NONE\"")
			}
		}
	}

	object Forge : NeoForgeLike("forge") {
		override val modManifestPath = "META-INF/mods.toml"

		override fun generateManifest(ctx: Context): String {
			val mcVersionRange = if (ctx.stonecutter.eval(ctx.stonecutterVersion, "<=" + ctx.currentMcVersion)) {
				val maxVersion = ctx.minecraftMaxVersion
				if (maxVersion == ctx.currentMcVersion && ctx.stonecutterVersion == ctx.currentMcVersion) {
					"[${ctx.stonecutterVersion},)"
				} else {
					"[${ctx.stonecutterVersion},${maxVersion}]"
				}
			} else {
				"[${ctx.currentMcVersion}]"
			}
			val forgeVersionRange = "[${ctx.currentMcVersion},)"

			return buildString {
				appendLine("modLoader = \"javafml\"")
				appendLine("loaderVersion = \"[47,)\"")
				appendLine("license = \"MIT\"")
				appendLine()
				appendLine("[[mods]]")
				appendLine("modId = \"${ctx.modId}\"")
				appendLine("version = \"${ctx.baseVersion}\"")
				appendLine("displayName = \"${ctx.modName}\"")
				appendLine("authors = \"${ctx.authors.firstOrNull() ?: "Starlev"}\"")
				appendLine("description = \"\"\"${ctx.description}\"\"\"")
				appendLine("logoFile = \"assets/${ctx.modId}/icon.png\"")
				appendLine()
				appendLine("[[dependencies.${ctx.modId}]]")
				appendLine("modId = \"forge\"")
				appendLine("mandatory = true")
				appendLine("versionRange = \"${forgeVersionRange}\"")
				appendLine("ordering = \"NONE\"")
				appendLine("side = \"BOTH\"")
				appendLine()
				appendLine("[[dependencies.${ctx.modId}]]")
				appendLine("modId = \"minecraft\"")
				appendLine("mandatory = true")
				appendLine("versionRange = \"${mcVersionRange}\"")
				appendLine("ordering = \"NONE\"")
				appendLine("side = \"BOTH\"")
			}
		}
	}

	companion object {
		fun of(id: String): Loader = when (id) {
			"fabric-o" -> FabricO
			"fabric-m" -> FabricM
			"fabric" -> FabricM
			"neoforge" -> NeoForge
			"forge" -> Forge
			else -> error("Unknown loader: '$id'")
		}
	}
}

private fun jsonStr(s: String): String = "\"${s.replace("\\", "\\\\").replace("\"", "\\\"")}\""
