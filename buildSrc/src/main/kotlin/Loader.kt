@file:Suppress("unused")

/**
 * Maven range for a pinned MC version, or a closed range when the variant spans several.
 *
 * A range with identical boundaries (`[1.19.2,1.19.2]`) is rejected by Forge's
 * MavenVersionAdapter ("Range cannot have identical boundaries") and aborts mod loading,
 * so an exact pin has to be spelled as the single-element form `[1.19.2]`.
 */
private fun exactOrRange(min: String, max: String): String =
	if (min == max) "[$min]" else "[$min,$max]"

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
			if (ctx.modrinthUrl.isNotBlank()) contact["homepage"] = ctx.modrinthUrl
			if (ctx.curseforgeUrl.isNotBlank()) contact["curseforge"] = ctx.curseforgeUrl

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
				appendLine("  \"license\": \"LGPL-3.0-only\",")
				if (ctx.credits.isNotBlank()) {
					appendLine("  \"custom\": {")
					appendLine("    \"lomka:credits\": ${jsonStr(ctx.credits)}")
					appendLine("  },")
				}
				appendLine("  \"environment\": \"*\",")
				appendLine("  \"accessWidener\": ${jsonStr("${ctx.stonecutterVersion}.accesswidener")},")
				appendLine("  \"entrypoints\": {")
				appendLine("    \"main\": [\"lomka.Lomka${'$'}Fabric\"]")
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
			"META-INF/mods.toml", "fabric.mod.json", ".cache"
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
			val mcVersionRange = when {
				ctx.hasMinecraftMin -> exactOrRange(ctx.minecraftMinVersion, ctx.minecraftMaxVersion)
				ctx.stonecutter.eval(ctx.stonecutterVersion, "<=" + ctx.currentMcVersion) -> {
					val maxVersion = ctx.minecraftMaxVersion
					if (maxVersion == ctx.currentMcVersion && ctx.stonecutterVersion == ctx.currentMcVersion) {
						"[${ctx.stonecutterVersion},)"
					} else {
						"[${ctx.stonecutterVersion},${maxVersion}]"
					}
				}
				else -> "[${ctx.currentMcVersion}]"
			}
			val neoforgeVersionRange = when {
				// 26.x NeoForge is still beta-only. Every 26.x release carries a "-beta" qualifier,
				// and Maven orders a qualified version below the same version without one, so a
				// floor of plain "26.x" rejects 26.x.0.0-beta - the first and lowest build of the
				// line - and the loader then refuses to start with "requires neoforge 26.x or above".
				ctx.currentMcVersion.startsWith("26.") -> "[${ctx.stonecutterVersion}.0-beta,)"
				ctx.stonecutter.eval(ctx.currentMcVersion, "<1.21") -> "[20,)"
				ctx.stonecutter.eval(ctx.currentMcVersion, "<1.21.10") -> "[21.0,)"
				else -> "[${ctx.currentMcVersion.removePrefix("1.")}-beta,)"
			}
			val displayUrl = ctx.modrinthUrl

			return buildString {
				appendLine("modLoader = \"javafml\"")
				appendLine("loaderVersion = \"[4,)\"")
				appendLine("license = \"LGPL-3.0-only\"")
				if (ctx.issuesUrl.isNotBlank()) appendLine("issueTrackerURL = ${tomlStr(ctx.issuesUrl)}")
				appendLine()
				appendLine("[[mods]]")
				appendLine("modId = \"${ctx.modId}\"")
				appendLine("version = \"${ctx.baseVersion}\"")
				appendLine("displayName = \"${ctx.modName}\"")
				appendLine("authors = \"${ctx.authors.firstOrNull() ?: "Starlev"}\"")
				if (displayUrl.isNotBlank()) appendLine("displayURL = ${tomlStr(displayUrl)}")
				if (ctx.curseforgeUrl.isNotBlank()) appendLine("modUrl = ${tomlStr(ctx.curseforgeUrl)}")
				if (ctx.credits.isNotBlank()) appendLine("credits = ${tomlStr(ctx.credits)}")
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
		override val excludedResources = listOf(
			"META-INF/neoforge.mods.toml", "fabric.mod.json", ".cache"
		)

		override fun generateManifest(ctx: Context): String {
			val mcVersionRange = when {
				ctx.hasMinecraftMin -> exactOrRange(ctx.minecraftMinVersion, ctx.minecraftMaxVersion)
				ctx.stonecutter.eval(ctx.stonecutterVersion, "<=" + ctx.currentMcVersion) -> {
					val maxVersion = ctx.minecraftMaxVersion
					if (maxVersion == ctx.currentMcVersion && ctx.stonecutterVersion == ctx.currentMcVersion) {
						"[${ctx.stonecutterVersion},)"
					} else {
						"[${ctx.stonecutterVersion},${maxVersion}]"
					}
				}
				else -> "[${ctx.currentMcVersion}]"
			}
			val forgeMajor = ctx.forgeMajor.ifBlank {
				if (ctx.stonecutter.eval(ctx.currentMcVersion, "<1.20")) "43" else "47"
			}
			val loaderVersionRange = "[$forgeMajor,)"
			val forgeVersionRange = "[$forgeMajor,)"
			val displayUrl = ctx.modrinthUrl

			return buildString {
				appendLine("modLoader = \"javafml\"")
				appendLine("loaderVersion = \"${loaderVersionRange}\"")
				appendLine("license = \"LGPL-3.0-only\"")
				if (ctx.issuesUrl.isNotBlank()) appendLine("issueTrackerURL = ${tomlStr(ctx.issuesUrl)}")
				appendLine()
				appendLine("[[mods]]")
				appendLine("modId = \"${ctx.modId}\"")
				appendLine("version = \"${ctx.baseVersion}\"")
				appendLine("displayName = \"${ctx.modName}\"")
				appendLine("authors = \"${ctx.authors.firstOrNull() ?: "Starlev"}\"")
				if (displayUrl.isNotBlank()) appendLine("displayURL = ${tomlStr(displayUrl)}")
				if (ctx.curseforgeUrl.isNotBlank()) appendLine("modUrl = ${tomlStr(ctx.curseforgeUrl)}")
				if (ctx.credits.isNotBlank()) appendLine("credits = ${tomlStr(ctx.credits)}")
				appendLine("description = \"\"\"${ctx.description}\"\"\"")
				// 1.19.2 loads the mod logo through AbstractPackResources#getRootResource(String),
				// which rejects any name containing '/' ("Root resources can only be filenames, not
				// paths"), so a path-style value throws the moment the entry is selected in the mod
				// list. The jar-root copy wired up in LomkaPlatform is referenced by bare filename;
				// 1.20.1+ takes varargs and accepts either form, so both Forge variants share this.
				appendLine("logoFile = \"logo.png\"")
				appendLine()
				appendLine("[[mixins]]")
				appendLine("config = \"${ctx.modId}.mixins.json\"")
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

private fun jsonStr(s: String): String = buildString {
	append('"')
	s.forEach { c ->
		when (c) {
			'\\' -> append("\\\\")
			'"' -> append("\\\"")
			'\u0008' -> append("\\b")
			'\t' -> append("\\t")
			'\n' -> append("\\n")
			'\u000C' -> append("\\f")
			'\r' -> append("\\r")
			else -> if (c.code < 0x20) {
				append("\\u")
				append(c.code.toString(16).padStart(4, '0'))
			} else {
				append(c)
			}
		}
	}
	append('"')
}

private fun tomlStr(s: String): String = jsonStr(s)
