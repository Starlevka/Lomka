















plugins {
	base
}

val jdk8: String = (findProperty("lomkaJdk8") as String?)
	?: System.getenv("LOMKA_JDK8")
	?: "${System.getProperty("user.home")}/.gradle/jdks/temurin-8-amd64-windows.2"

val wrapper = if (System.getProperty("os.name").startsWith("Windows")) "gradlew.bat" else "gradlew"


fun registerLegacyBuild(name: String, dir: String, gradleTask: String, javaHome: String?) =
	tasks.register<Exec>(name) {
		group = "legacy"
		description = "Runs '$gradleTask' in $dir" + (javaHome?.let { " with JDK 8 at $it" } ?: "")
		workingDir = file(dir)
		commandLine(listOf(wrapper, gradleTask))
		if (javaHome != null) {
			doFirst {
				if (!file(javaHome).isDirectory) {
					throw GradleException(
						"JDK 8 not found at $javaHome. Pass -PlomkaJdk8=<path to a JDK 8> or set LOMKA_JDK8."
					)
				}
			}
			environment("JAVA_HOME", javaHome)
		}
	}

val legacy110 = registerLegacyBuild("legacy110", "legacy-forge-1.7.10", "reobf", jdk8)
val legacy189 = registerLegacyBuild("legacy189", "legacy-forge-1.8.9", "reobfJar", jdk8)
val legacy112 = registerLegacyBuild("legacy112", "legacy-forge-1.12.2", "reobfJar", null)

tasks.register("buildLegacy") {
	group = "legacy"
	description = "Builds the reobfuscated jar for 1.7.10, 1.8.9 and 1.12.2, in that order."
	dependsOn(legacy110, legacy189, legacy112)
}