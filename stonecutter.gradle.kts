@file:OptIn(dev.kikugie.stonecutter.StonecutterExperimentalAPI::class)

plugins {
	alias(libs.plugins.stonecutter)
	alias(libs.plugins.fabric.loom).apply(false)
	alias(libs.plugins.fabric.loom.remap).apply(false)
}

stonecutter active file(".sc_active_version")

tasks.register("runActiveClient") {
	group = "stonecutter"
	description = "Run client of the active Stonecutter version"
	dependsOn(stonecutter.current!!.project + ":runClient")
}

tasks.register("runActiveServer") {
	group = "stonecutter"
	description = "Run server of the active Stonecutter version"
	dependsOn(stonecutter.current!!.project + ":runServer")
}

stonecutter parameters {
	swaps["mod_version"] = "\"${properties.get<String>("mod.version")}\";"
	swaps["mod_id"] = "\"${properties.get<String>("mod.id")}\";"
	swaps["mod_name"] = "\"${properties.get<String>("mod.name")}\";"
	swaps["mod_group"] = "\"${properties.get<String>("mod.group")}\";"
	swaps["minecraft"] = "\"${current.version}\";"
	constants["release"] = properties.get<String>("mod.id") == "lomka"

	// Loader gates for the unified Lomka entrypoint (see lomka/Lomka.java).
	val lomkaLoader = current.project.substringAfterLast('-')
	constants["fabric"] = lomkaLoader == "fabric"
	constants["forge"] = lomkaLoader == "forge"
	constants["neoforge"] = lomkaLoader == "neoforge"

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

tasks.register("buildAllVariants") {
	group = "build"
	description = "Build all Stonecutter variants"
	dependsOn(stonecutter.tasks.named("buildAndCollect"))
}

stonecutter.tasks.order("buildAndCollect")

val lomkaLicenseHeader = """
/*
 * This file is part of Lomka (https://github.com/Starlevka/Lomka)
 * Copyright (C) 2026 Starlev (a.k.a. Starlevka) and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: LGPL-3.0-only
 */
""".trimIndent()

val lomkaJavaSources = layout.projectDirectory.dir("src/main/java").asFile

// Adds or rewrites the license header on every Java source. Runs automatically before each
// variant's compileJava (see lomkaPlatform), so new classes and edited header wording both
// converge to the current template without manual steps. A stale header is detected by an
// exact prefix match against the current template; any leading block comment containing an
// SPDX line is treated as the old header and replaced.
tasks.register("licenseHeaders") {
	group = "build"
	description = "Adds or rewrites the Lomka license header on every Java source."
	doLast {
		var updated = 0
		lomkaJavaSources.walkTopDown()
			.filter { it.isFile && it.extension == "java" }
			.forEach { file ->
				val text = file.readText(Charsets.UTF_8).replace("\r\n", "\n")
				if (text.startsWith(lomkaLicenseHeader)) return@forEach
				var rest = text
				if (rest.startsWith("/*")) {
					val end = rest.indexOf("*/")
					if (end > 0 && rest.substring(0, end).contains("SPDX-License-Identifier")) {
						rest = rest.substring(end + 2).trimStart('\n')
					}
				}
				file.writeText(lomkaLicenseHeader + "\n\n" + rest, Charsets.UTF_8)
				updated++
			}
		if (updated > 0) {
			println("Lomka license header written to $updated file(s)")
		}
	}
}
