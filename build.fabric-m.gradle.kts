plugins {
	alias(libs.plugins.fabric.loom)
}

lomkaPlatform(Loader.FabricM)

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

dependencies {
	minecraft("com.mojang:minecraft:${prop("deps.minecraft")}")
	implementation("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}")
}
