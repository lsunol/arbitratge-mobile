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

// Vectors compartits Android ↔ PWA (02 §3.7.11) a la carpeta pública `test-vectors/`.
val testVectorsDir = rootProject.layout.projectDirectory.dir("test-vectors")

tasks.test {
    inputs.dir(testVectorsDir).withPropertyName("testVectors")
    systemProperty("arb1.testVectorsDir", testVectorsDir.asFile.absolutePath)
}

// Regenera `test-vectors/cases.json` amb les claus fixes de `test-vectors/test-keys.json`.
// Reproduïble: aleatorietat amb llavor fixa (mateix JDK ⇒ mateixos bytes). Vegeu test-vectors/README.md.
tasks.register<JavaExec>("generateTestVectors") {
    group = "verification"
    description = "Regenera els vectors públics ARB1 de test-vectors/ (claus de prova fixes)."
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("cat.futbolbotons.arbitratge.credential.vectors.GenerateTestVectorsKt")
    args(testVectorsDir.asFile.absolutePath)
    // Només el primer cop: ./gradlew :core-credential:generateTestVectors -PinitKeys
    if (providers.gradleProperty("initKeys").isPresent) args("--init-keys")
}
