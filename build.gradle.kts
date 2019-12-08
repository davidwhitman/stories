import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/////////////////
// ATTN: CHANGE ME
val starsectorDirectory = "C:/Program Files (x86)/Fractal Softworks/Starsector"
val jarFileName = "Stories.jar"
/////////////////

val starsectorCoreDirectory = "$starsectorDirectory/starsector-core"
val starsectorModDirectory = "$starsectorDirectory/mods"

plugins {
    kotlin("jvm") version "1.3.60"
    java
}

version = "1.0.0"

repositories {
    maven(url = uri("$projectDir/libs"))
    jcenter()
}

dependencies {
    val kotlinVersionInLazyLib = "1.3.61"

    // Get kotlin sdk from LazyLib during runtime, only use it here during compile time
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersionInLazyLib")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersionInLazyLib")


    compileOnly(fileTree("$starsectorModDirectory/LazyLib/jars") { include("*.jar") })
    compileOnly(fileTree("$starsectorModDirectory/Console Commands/jars") { include("*.jar") })

    // Starsector jars and dependencies
    compileOnly(fileTree(starsectorCoreDirectory) {
        include("*.jar")
//        exclude("*_obf.jar")
    })

    // Unit testing libraries. Good to use, but not necessary.
//    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.2")
//    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.2")
//    testImplementation("org.assertj:assertj-core:3.11.1")
//    testImplementation("io.mockk:mockk:1.9")
}

tasks {
    named<Jar>("jar")
    {
        destinationDir = file("$rootDir/jars")
        archiveName = jarFileName
    }

    register("debug-starsector", Exec::class) {
        println("Starting debugger for Starsector...")
        workingDir = file(starsectorCoreDirectory)


        commandLine = if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            listOf("cmd", "/C", "debug-starsector.bat")
        } else {
            listOf("./starsectorDebug.bat")
        }
    }

    register("run-starsector", Exec::class) {
        println("Starting Starsector...")
        workingDir = file(starsectorCoreDirectory)

        commandLine = if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            listOf("cmd", "/C", "starsector.bat")
        } else {
            listOf("./starsector.bat")
        }
    }
}

// Compile to Java 6 bytecode so that Starsector can use it
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.6"
}