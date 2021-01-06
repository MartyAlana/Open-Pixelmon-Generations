rootProject.name = "OpenPixelmonGenerations"

pluginManagement {
	repositories {
		gradlePluginPortal()
		jcenter()

		maven {
			name = "MinecraftForge"
			url = uri("https://files.minecraftforge.net/maven/")
		}
	}

	resolutionStrategy {
		eachPlugin {
			 if (requested.id.id == "net.minecraftforge.gradle") {
			  	useModule("net.minecraftforge.gradle:ForgeGradle:3.0.190")
			 }
		}
	}
}
