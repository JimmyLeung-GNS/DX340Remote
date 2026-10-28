pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name = "DX340Remote"
include(":core-protocol", ":core-ble", ":source-adapters", ":library-cache", ":host-app", ":remote-app")
include(":ui-probe-app")
include(":controller-core")
