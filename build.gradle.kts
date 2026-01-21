plugins {
    kotlin("jvm") version "1.9.22"
    application
}

repositories {
    mavenCentral()
}

val gdxVersion = "1.12.1"
val ktxVersion = "1.12.1-rc1"

dependencies {
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    
    implementation("io.github.libktx:ktx-app:$ktxVersion")
    implementation("io.github.libktx:ktx-graphics:$ktxVersion")
    implementation("io.github.libktx:ktx-math:$ktxVersion")
}

application {
    mainClass.set("com.mags.MainKt")
    applicationDefaultJvmArgs = if (System.getProperty("os.name").lowercase().contains("mac")) {
        listOf("-XstartOnFirstThread")
    } else {
        emptyList()
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
