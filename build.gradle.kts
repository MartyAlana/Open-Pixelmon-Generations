import uk.jamierocks.propatcher.task.ApplyPatchesTask
import uk.jamierocks.propatcher.task.MakePatchesTask
import uk.jamierocks.propatcher.task.ResetSourcesTask

plugins {
	java
	id("net.minecraftforge.gradle")
	id("uk.jamierocks.propatcher") version "1.3.2" apply false
}

version = "8.2.2"
group = "com.pixelmongenerations"

val bon2DownloadUrl = "https://ci.tterrag.com/job/BON2/lastSuccessfulBuild/artifact/build/libs/BON-2.4.0.15-all.jar"

repositories {
	maven {
		url = uri("https://maven.fabricmc.net/")
	}

	maven {
		url = uri("http://maven.shadowfacts.net/")
	}

	maven {
		url = uri("http://maven.bluexin.be/repository/snapshots/")
	}

	maven {
		url = uri("https://jitpack.io")
	}

	ivy {
		url = uri("https://ci.tterrag.com/job/BON2/")

		layout("pattern") {
			this as IvyPatternRepositoryLayout
			artifact("/[organisation]/artifact/build/libs/[module]-[revision]-[classifier].[ext]")
		}
	}
}

val decompiler by configurations.creating
val bon2 by configurations.creating

dependencies {
	minecraft("net.minecraftforge", "forge", "1.12.2-14.23.5.2854")

	implementation("com.github.thecodewarrior", "BinarySMD", "-SNAPSHOT")
	implementation("org.msgpack", "msgpack-core", "0.8.17")
	implementation("org.jetbrains", "annotations", "17.0.0")
	implementation("com.typesafe", "config", "1.4.1")
	implementation("com.github.thecodewarrior", "bitfont", "b8251e7ba0")
	implementation("com.github.Vatuu", "discord-rpc", "1.6.2")
	implementation("ninja.leaping.configurate", "configurate-hocon", "3.3")
	implementation("ninja.leaping.configurate", "configurate-parent", "3.7.1", ext = "pom")

	implementation(fg.deobf("net.shadowfacts:Forgelin:1.8.4"))
	implementation(fg.deobf("com.teamwizardry.librarianlib:librarianlib-1.12.2:v4.21-4.20-SNAPSHOT")).apply {
		this as ExternalModuleDependency
		isChanging = true
		exclude(group = "com.github.thecodewarrior")
	}

	decompiler("com.github.fesh0r", "fernflower", "dbf407a655")
	bon2("lastSuccessfulBuild", "BON", "2.4.0.15", classifier = "all")
}

minecraft {
	mappings("snapshot", "20171003-1.12")

	file("src/main/resources/META-INF/pixelmon_at.cfg").apply {
		if (exists()) {
			accessTransformers.add(this)
		}
	}

	runs {
		create("client") {
			workingDirectory = "run/client"
			property("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")
			property("forge.logging.console.level", "debug")
		}

		create("server") {
			workingDirectory = "run/server"

			property("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")
			property("forge.logging.console.level", "debug")
		}
	}
}

base {
	archivesBaseName = "PixelmonGenerations-1.12.2"
}

java {
	sourceCompatibility = JavaVersion.VERSION_1_8
	targetCompatibility = JavaVersion.VERSION_1_8
}

val resetSources by tasks.registering(ResetSourcesTask::class) {
	root = rootProject.file(".gradle/decompiled/sources")
	target = file("src/main/java")
}

val applySourcePatches by tasks.registering(ApplyPatchesTask::class) {
	dependsOn(resetSources)

	target = file("src/main/java")
	patches = file("patches/java")
}

val makeSourcePatches by tasks.registering(MakePatchesTask::class) {
	root = rootProject.file(".gradle/decompiled/sources")
	target = file("src/main/java")
	patches = file("patches/java")
}

val resetResources by tasks.registering(ResetSourcesTask::class) {
	root = rootProject.file(".gradle/decompiled/resources")
	target = file("src/main/resources")
}

val applyResourcePatches by tasks.registering(ApplyPatchesTask::class) {
	dependsOn(resetResources)

	target = file("src/main/resources")
	patches = file("patches/resources")

}

val makeResourcePatches by tasks.registering(MakePatchesTask::class) {
	root = rootProject.file(".gradle/decompiled/resources")
	target = file("src/main/resources")
	patches = file("patches/resources")
}

val reset by tasks.registering {
	dependsOn(resetSources, resetResources)
}

val apply by tasks.registering {
	dependsOn(applySourcePatches, applyResourcePatches)
}

val makePatches by tasks.registering {
	dependsOn(makeSourcePatches, makeResourcePatches)
}

val setup by tasks.registering {
	doLast {
		File(".gradle/decompiled").mkdirs()
		File("build/decompiled").mkdirs()

		logger.info("Remapping")

		javaexec {
			main = "com.github.parker8283.bon2.BON2"
			classpath(bon2)
			args("--inputJar", "pixelmon.jar", "--outputJar", "build/pixelcrab.jar", "--mappingsVer", "1.12-snapshot_20180814")
		}

		logger.info("Decompiling")

		javaexec {
			main = "org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler"
			classpath(decompiler)
			args("-din=1", "-dgs=1", "-asc=1", "-rsy=1", "-log=TRACE", "-ind=    ")
			args("build/pixelcrab.jar", "build/decompiled/")
		}

		logger.info("Copying sources")

		copy {
			includeEmptyDirs = false
			include("com/pixelmongenerations/**/*.java")
			from(zipTree("build/decompiled/pixelcrab.jar"))
			into(rootProject.file(".gradle/decompiled/sources"))
		}

		logger.info("Copying resources")

		copy {
			includeEmptyDirs = false
			include("assets/**/*")
			include("mcmod.info")
			include("pack.mcmeta")
			include("META-INF/pixelmon_at.cfg")
			from(zipTree("build/decompiled/pixelcrab.jar"))
			into(rootProject.file(".gradle/decompiled/resources"))
		}
	}
}

tasks.withType<JavaCompile> {
	options.encoding = "UTF-8"
	sourceCompatibility = "8"
	targetCompatibility = "8"

	if (JavaVersion.current().isJava9Compatible) {
		logger.info("This build might not be compatible with Java 8")
	}
}

tasks.named<Jar>("jar") {
	manifest {
		attributes(mapOf(
				"FMLCorePluginContainsFMLMod" to "true",
				"FMLAT" to "pixelmon_at.cfg",
				"FMLCorePlugin" to "com.pixelmongenerations.core.plugin.EarlyLoadPlugin"
		))
	}
}
