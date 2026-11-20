// Kotlin/JVM pur: sense dependències d'Android ni de core-credential (03 §3.1).
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
}

// El banco de muestra es entrada declarada de los tests (se reejecutan si cambia).
val sampleBankFile = rootProject.layout.projectDirectory.file("bank-sample/bank-sample.json")
tasks.test {
    inputs.file(sampleBankFile).withPropertyName("sampleBank").withPathSensitivity(PathSensitivity.NONE)
    systemProperty("bank.sample.path", sampleBankFile.asFile.absolutePath)
}
