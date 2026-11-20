// Kotlin/JVM pur: contracte ARB1 (02 §3.7). Sense dependències d'Android ni de core-exam.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
}
