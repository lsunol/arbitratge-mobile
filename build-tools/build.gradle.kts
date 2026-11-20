// Herramientas de build (packBank / packSealKey). Usa core-exam (validador) y core-credential (P-256
// canónico) en solo lectura; NO se empaqueta en el APK.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core-exam"))
    implementation(project(":core-credential"))
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
}

val sampleBankFile = rootProject.layout.projectDirectory.file("bank-sample/bank-sample.json")
val testKeysFile = rootProject.layout.projectDirectory.file("test-vectors/test-keys.json")
val devKeyFile = rootProject.layout.projectDirectory.file("app-candidate/src/debug/seal/dev-seal-key.der")
tasks.test {
    inputs.files(sampleBankFile, testKeysFile, devKeyFile).withPropertyName("fixtures").withPathSensitivity(PathSensitivity.NONE)
    systemProperty("bank.sample.path", sampleBankFile.asFile.absolutePath)
    systemProperty("test.keys.path", testKeysFile.asFile.absolutePath)
    systemProperty("dev.key.path", devKeyFile.asFile.absolutePath)
}
