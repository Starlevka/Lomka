@file:Suppress("unused")

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import javax.inject.Inject

abstract class ModPlatformExtension {
	abstract val loader: Property<String>
	abstract val jarTask: Property<String>
	abstract val sourcesJarTask: Property<String>

	@get:Nested
	abstract val dependencies: DependenciesConfig

	init {
		loader.convention("fabric")
		jarTask.convention("jar")
		sourcesJarTask.convention("sourcesJar")
	}

	fun dependencies(action: Action<DependenciesConfig>) {
		action.execute(dependencies)
	}
}

abstract class DependenciesConfig @Inject constructor(val objects: ObjectFactory) {
	private fun container() = objects.domainObjectContainer(Dependency::class.java)

	val required: NamedDomainObjectContainer<Dependency> = container()

	fun required(modid: String, action: Action<Dependency>): Dependency? = required.create(modid, action)
}

abstract class Dependency @Inject constructor(val name: String) {
	abstract val modid: Property<String>
	abstract val fabricLikeVersionRange: Property<String>

	init {
		modid.convention(name)
		fabricLikeVersionRange.convention("*")
	}

	fun slug(slug: String) {
		// no-op for now
	}
}
