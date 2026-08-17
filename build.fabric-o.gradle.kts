plugins {
	alias(libs.plugins.fabric.loom.remap)
}

lomkaPlatform(Loader.FabricO)

val awFile = lomkaAwFile()

loom {
	accessWidenerPath = awFile
	runs.named("client") {
		client()
		ideConfigGenerated(true)
		runDir = "run/"
		environment = "client"
		programArgs("--username=Dev")
		configName = "Fabric Client"
	}
	runs.named("server") {
		server()
		ideConfigGenerated(true)
		runDir = "run/"
		environment = "server"
		configName = "Fabric Server"
	}
}

sourceSets["main"].resources.srcDir(awFile.parentFile)

repositories {
	mavenCentral()
}

configurations.all {
	resolutionStrategy {
		force("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
	}
}

dependencies {
	minecraft("com.mojang:minecraft:${prop("deps.minecraft")}")
	mappings(loom.layered { officialMojangMappings() })
	modImplementation("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
	if (sc.current.parsed < "1.21.11") {
		compileOnly("org.jspecify:jspecify:1.0.0")
	}
}
