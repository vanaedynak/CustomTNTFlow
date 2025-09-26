plugins {
    java
}

group = "dev.byflow.customtntflow"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9")
    compileOnly("com.sk89q.worldguard:worldguard-core:7.0.9")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.15")
    compileOnly("de.tr7zw:item-nbt-api:2.12.3")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<ProcessResources>().configureEach {
    filteringCharset = "UTF-8"
}

tasks.register<Copy>("copyPlugin") {
    dependsOn(tasks.named("jar"))
    from(tasks.named("jar"))
    into(layout.projectDirectory.dir("build/output"))
}
