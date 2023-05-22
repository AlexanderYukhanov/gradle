plugins {
    id("gradlebuild.distribution.packaging")
    id("gradlebuild.verify-build-environment")
    id("gradlebuild.install")
    id("org.spdx.sbom") version "0.1.0"
}

description = "The collector project for the entirety of the Gradle distribution"

dependencies {
    coreRuntimeOnly(platform(project(":core-platform")))

    agentsRuntimeOnly(project(":instrumentation-agent"))

    pluginsRuntimeOnly(platform(project(":distributions-publishing")))
    pluginsRuntimeOnly(platform(project(":distributions-jvm")))
    pluginsRuntimeOnly(platform(project(":distributions-native")))

    pluginsRuntimeOnly(project(":plugin-development"))
    pluginsRuntimeOnly(project(":build-init"))
    pluginsRuntimeOnly(project(":build-profile"))
    pluginsRuntimeOnly(project(":antlr"))
    pluginsRuntimeOnly(project(":enterprise"))
}

tasks.register<gradlebuild.run.tasks.RunEmbeddedGradle>("runDevGradle") {
    group = "verification"
    description = "Runs an embedded Gradle using the partial distribution for ${project.path}."
    gradleClasspath.from(configurations.runtimeClasspath.get(), tasks.runtimeApiInfoJar)
}

// This is required for the separate promotion build and should be adjusted there in the future
tasks.register<Copy>("copyDistributionsToRootBuild") {
    dependsOn("buildDists")
    from(layout.buildDirectory.dir("distributions"))
    into(rootProject.layout.buildDirectory.dir("distributions"))
}

spdxSbom {
  targets {
    create("release") {
      configurations.set(listOf("runtimeClasspath"))
      scm {
        uri.set("github.com/gradle/gradle")
        revision.set(providers.exec {
          commandLine("sh", "-c", "git rev-parse --verify HEAD | tr -d '\n'")
        }.standardOutput.asText)
      }
      document {
        name.set("Gradle " + project.version)
        namespace.set("https://gradle.org/spdx/gradle-" + project.version + "-bin.zip")
        creator.set("Organization: Alex")
        uberPackage {
          name.set("gradle-" + project.version + "-bin.zip")
          version.set(project.version.toString())
          supplier.set("Organization: Gradle")
        }
      }
    }
  }
}

tasks.withType<org.spdx.sbom.gradle.SpdxSbomTask>().configureEach {
  taskExtension.set(object : org.spdx.sbom.gradle.extensions.DefaultSpdxSbomTaskExtension() {
    override fun shouldCreatePackageForProject(projectInfo: org.spdx.sbom.gradle.project.ProjectInfo): Boolean {
      return false
    }}
  )
}
