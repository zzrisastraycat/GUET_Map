pluginManagement {
    repositories {
        // 国内镜像优先，避免 Gradle Plugin Portal 超时
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // 高德 SDK（3dmap / search）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://a.amap.com/lbs/static/unzip/AndroidMapSDK/android_studio/") }

    }
}

rootProject.name = "GUET_Map"
include(":app")
