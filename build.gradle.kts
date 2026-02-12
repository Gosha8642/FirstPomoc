// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("gradle.plugin.com.onesignal:onesignal-gradle-plugin:[0.14.0, 0.99.99]")
        classpath("com.google.gms:google-services:4.4.2")
    }
}