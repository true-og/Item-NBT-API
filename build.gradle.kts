import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.AppendingTransformer
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.language.jvm.tasks.ProcessResources

/* ------------------------------ Plugins ------------------------------ */
plugins {
    id("java") // Import Java plugin.
    id("java-library") // Import Java Library plugin.
    id("com.diffplug.spotless") version "7.0.4" // Import Spotless plugin.
    id("com.gradleup.shadow") version "8.3.8" // Import Shadow plugin.
    id("checkstyle") // Import Checkstyle plugin.
    eclipse // Import Eclipse plugin.
    kotlin("jvm") version "2.1.21" // Import Kotlin JVM plugin.
}

extra["kotlinAttribute"] = Attribute.of("kotlin-tag", Boolean::class.javaObjectType)

val kotlinAttribute: Attribute<Boolean> by rootProject.extra

/* --------------------------- JDK / Kotlin ---------------------------- */
java {
    sourceCompatibility = JavaVersion.VERSION_17 // Compile with JDK 17 compatibility.
    toolchain { // Select Java toolchain.
        languageVersion.set(JavaLanguageVersion.of(17)) // Use JDK 17.
        vendor.set(JvmVendorSpec.GRAAL_VM) // Use GraalVM CE.
    }
}

kotlin { jvmToolchain(17) }

/* ----------------------------- Metadata ------------------------------ */
group = "de.tr7zw" // Declare bundle identifier.

version = "2.14.2" // Declare plugin version (will be in .jar).

val apiVersion = "1.19" // Declare minecraft server target version.

/* ----------------------------- Resources ----------------------------- */
tasks.named<ProcessResources>("processResources") {
    val props = mapOf("version" to version, "apiVersion" to apiVersion)
    inputs.properties(props) // Indicates to rerun if version changes.
    filesMatching("plugin.yml") { expand(props) }
    from("LICENSE") { into("/") } // Bundle licenses into jarfiles.
}

/* ---------------------------- Repos ---------------------------------- */
repositories {
    mavenCentral() // Import the Maven Central Maven Repository.
    gradlePluginPortal() // Import the Gradle Plugin Portal Maven Repository.
    maven { url = uri("https://repo.purpurmc.org/snapshots") } // Import the PurpurMC Maven Repository.
    maven { url = uri("https://repo.codemc.org/repository/maven-public/") }
    maven { url = uri("https://repo.codemc.org/repository/nms/") }
    maven { url = uri("https://libraries.minecraft.net/") }
    maven { url = uri("file://${System.getProperty("user.home")}/.m2/repository") }
    System.getProperty("SELF_MAVEN_LOCAL_REPO")?.let {
        val dir = file(it)
        if (dir.isDirectory) {
            println("Using SELF_MAVEN_LOCAL_REPO at: $it")
            maven { url = uri("file://${dir.absolutePath}") }
        } else {
            logger.error("TrueOG Bootstrap not found, defaulting to ~/.m2 for mavenLocal()")
            mavenLocal()
        }
    } ?: logger.error("TrueOG Bootstrap not found, defaulting to ~/.m2 for mavenLocal()")
}

/* ---------------------- Java project deps ---------------------------- */
dependencies {
    compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT") // Declare Purpur API version to be packaged.
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "checkstyle")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "eclipse")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://repo.purpurmc.org/snapshots") }
        maven { url = uri("https://repo.codemc.org/repository/maven-public/") }
        maven { url = uri("https://repo.codemc.org/repository/nms/") }
        maven { url = uri("https://libraries.minecraft.net/") }
        maven { url = uri("file://${System.getProperty("user.home")}/.m2/repository") }
        System.getProperty("SELF_MAVEN_LOCAL_REPO")?.let {
            val dir = file(it)
            if (dir.isDirectory) {
                println("Using SELF_MAVEN_LOCAL_REPO at: $it")
                maven { url = uri("file://${dir.absolutePath}") }
            } else {
                logger.error("TrueOG Bootstrap not found, defaulting to ~/.m2 for mavenLocal()")
                mavenLocal()
            }
        } ?: logger.error("TrueOG Bootstrap not found, defaulting to ~/.m2 for mavenLocal()")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
            vendor.set(JvmVendorSpec.GRAAL_VM)
        }
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<ProcessResources>().configureEach {
        val props = mapOf("version" to rootProject.version, "apiVersion" to apiVersion)
        inputs.properties(props)
        filesMatching("plugin.yml") { expand(props) }
        from(rootProject.file("LICENSE")) { into("/") }
    }

    dependencies {
        compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT")
        compileOnly("com.mojang:datafixerupper:4.0.26")
        compileOnly("com.mojang:authlib:1.5.25")
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }

    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.add("-parameters")
        options.isFork = true
        options.compilerArgs.add("-Xlint:deprecation")
        options.encoding = "UTF-8"
    }

    spotless {
        java {
            eclipse().configFile(rootProject.file("config/formatter/eclipse-java-formatter.xml"))
            leadingTabsToSpaces()
            removeUnusedImports()
        }
        kotlinGradle {
            ktfmt().kotlinlangStyle().configure { it.setMaxWidth(120) }
            target("build.gradle.kts", "settings.gradle.kts")
        }
    }

    checkstyle {
        toolVersion = "10.18.1"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        isIgnoreFailures = true
        isShowViolations = true
    }

    tasks.named("compileJava") { dependsOn("spotlessApply") }
    tasks.named("spotlessCheck") { dependsOn("spotlessApply") }

    tasks.named<Jar>("jar") { archiveClassifier.set("part") }
}

/* ---------------------- Reproducible jars ---------------------------- */
tasks.withType<AbstractArchiveTask>().configureEach { // Ensure reproducible .jars
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

/* ----------------------------- Shadow -------------------------------- */
tasks.shadowJar {
    archiveClassifier.set("") // Use empty string instead of null.
    minimize()
}

tasks.jar { archiveClassifier.set("part") } // Applies to root jarfile only.

tasks.build { dependsOn(tasks.spotlessApply, tasks.shadowJar) } // Build depends on spotless and shadow.

/* --------------------------- Javac opts ------------------------------- */
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters") // Enable reflection for java code.
    options.isFork = true // Run javac in its own process.
    options.compilerArgs.add("-Xlint:deprecation") // Trigger deprecation warning messages.
    options.encoding = "UTF-8" // Use UTF-8 file encoding.
}

/* ----------------------------- Auto Formatting ------------------------ */
spotless {
    java {
        eclipse().configFile("config/formatter/eclipse-java-formatter.xml") // Eclipse java formatting.
        leadingTabsToSpaces() // Convert leftover leading tabs to spaces.
        removeUnusedImports() // Remove imports that aren't being called.
    }
    kotlinGradle {
        ktfmt().kotlinlangStyle().configure { it.setMaxWidth(120) } // JetBrains Kotlin formatting.
        target("build.gradle.kts", "settings.gradle.kts") // Gradle files to format.
    }
}

checkstyle {
    toolVersion = "10.18.1" // Declare checkstyle version to use.
    configFile = file("config/checkstyle/checkstyle.xml") // Point checkstyle to config file.
    isIgnoreFailures = true // Don't fail the build if checkstyle does not pass.
    isShowViolations = true // Show the violations in any IDE with the checkstyle plugin.
}

tasks.named("compileJava") {
    dependsOn("spotlessApply") // Run spotless before compiling with the JDK.
}

tasks.named("spotlessCheck") {
    dependsOn("spotlessApply") // Run spotless before checking if spotless ran.
}

/* ------------------------------ Eclipse SHIM ------------------------- */

// This can't be put in eclipse.gradle.kts because Gradle is weird.
subprojects {
    apply(plugin = "java-library")
    apply(plugin = "eclipse")
    eclipse.project.name = "${project.name}-${rootProject.name}"
    tasks.withType<Jar>().configureEach { archiveBaseName.set("${project.name}-${rootProject.name}") }
}

/* --------------------------- Module wiring --------------------------- */

project(":item-nbt-api") {
    tasks.withType<Jar>().configureEach { archiveBaseName.set("item-nbt-api") }
    dependencies {}
    tasks.named<ShadowJar>("shadowJar") {
        archiveClassifier.set("")
        minimize()
        transform(AppendingTransformer::class.java) { resource = "LICENSE" }
    }
    tasks.named("build") { dependsOn("spotlessApply", "shadowJar") }
}

project(":item-nbt-plugin") {
    tasks.withType<Jar>().configureEach { archiveBaseName.set("Item-NBT-API") }
    dependencies { api(project(":item-nbt-api")) }
    tasks.named<ShadowJar>("shadowJar") {
        archiveClassifier.set("")
        minimize()
        relocate("de.tr7zw.changeme.nbtapi", "de.tr7zw.nbtapi")
        relocate("javassist", "de.tr7zw.nbtinjector.javassist")
        relocate("de.tr7zw.annotations", "de.tr7zw.nbtapi.utils.annotations")
        transform(AppendingTransformer::class.java) { resource = "LICENSE" }
        dependsOn(project(":item-nbt-api").tasks.named("jar"))
    }
    val copyJar =
        tasks.register<Copy>("copyJar") {
            from(tasks.named("shadowJar"))
            into(rootProject.layout.projectDirectory.dir("target"))
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    tasks.named("build") {
        dependsOn("spotlessApply", "shadowJar")
        finalizedBy(copyJar)
    }

    plugins.withId("java-library") {
        val aggregateJavadoc =
            tasks.register<Javadoc>("aggregateJavadoc") {
                val srcSets = project.extensions.getByType(SourceSetContainer::class.java)
                val thisSrc = srcSets.named("main").get()
                val apiSrcSets = project(":item-nbt-api").extensions.getByType(SourceSetContainer::class.java)
                val apiSrc = apiSrcSets.named("main").get()
                source(thisSrc.allJava)
                source(apiSrc.allJava)
                classpath = thisSrc.compileClasspath + apiSrc.compileClasspath
                isFailOnError = false
            }
        tasks.register<Jar>("aggregateJavadocJar") {
            dependsOn(aggregateJavadoc)
            from(aggregateJavadoc.get().destinationDir)
            archiveClassifier.set("javadoc")
        }
    }
}

project(":mappings-parser") {
    tasks.withType<Jar>().configureEach { archiveBaseName.set("mappings-parser") }
    dependencies { implementation(project(":item-nbt-api")) }
    tasks.named<ShadowJar>("shadowJar") {
        archiveClassifier.set("")
        minimize()
        transform(AppendingTransformer::class.java) { resource = "LICENSE" }
        dependsOn(project(":item-nbt-api").tasks.named("jar"))
    }
    tasks.named("build") { dependsOn("spotlessApply", "shadowJar") }
}
