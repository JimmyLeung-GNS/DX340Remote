plugins { kotlin("jvm") }
kotlin { jvmToolchain(21) }
dependencies { testImplementation("junit:junit:4.13.2") }
dependencies { testImplementation("org.json:json:20180813") }
sourceSets.test { resources.srcDir("../../protocol/fixtures") }
