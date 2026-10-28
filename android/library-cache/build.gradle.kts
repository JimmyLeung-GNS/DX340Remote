plugins { kotlin("jvm") }
kotlin { jvmToolchain(21) }
dependencies { testImplementation("junit:junit:4.13.2") }
dependencies {
    testImplementation(project(":core-protocol"))
    testImplementation("org.json:json:20180813")
}

tasks.register<JavaExec>("metadataBenchmark") {
    group = "verification"
    description = "Measure synthetic metadata components on the local JVM; not source/BLE latency."
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("org.dx340.remote.library.MetadataBenchmark")
    args(providers.gradleProperty("metadataReport").getOrElse("../../artifacts/library-metadata/summary.json"))
}
