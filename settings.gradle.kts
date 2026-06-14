pluginManagement {
    repositories {
        // 官方源优先
        google()
        mavenCentral()
        gradlePluginPortal()
        // 国内镜像作为备用
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 官方源优先
        google()
        mavenCentral()

        // 高德 SDK（3dmap / search）
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://a.amap.com/lbs/static/unzip/AndroidMapSDK/android_studio/") }

    }
}

rootProject.name = "GUET_Map"
include(":app")
