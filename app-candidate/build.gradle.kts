plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// ---------------------------------------------------------------------------------------------
// Inyección del banco y de la clave de sello (03 §5.4, §6.4; 05 §4.3.1).
//
//  · debug  : SIEMPRE `bank-sample/bank-sample.json` (ARB_BANK_FILE se ignora con aviso) y la clave pública por
//             diseño `kid=dev` de `src/debug/seal/`.
//  · release: EXIGE ARB_BANK_FILE, ARB_SEAL_KEY_FILE y ARB_SEAL_KID (variable de entorno o
//             propiedad Gradle) y `--no-build-cache` (si no, falla: el banco/clave reales no deben entrar
//             en la caché de build). Rechaza la clave dev/test de debug y de test-vectors aunque cambie el
//             kid. Sin ellas, o con `sample=true`, la tarea FALLA con un mensaje claro.
//
// Las tareas nunca imprimen texto del banco ni de la clave: solo recuentos, IDs y el `kid`.
// Se ejecutan con las clases de `core-exam` (BuildTools) y NO se cachean (`DisableCachingByDefault`):
// la entrada puede ser el banco real y no debe acabar en la caché de build.
// ---------------------------------------------------------------------------------------------

val buildTools = configurations.create("buildTools") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    buildTools(project(":build-tools"))
}

/** Entorno o propiedad Gradle (la propiedad manda). Proveedor compatible con la caché de configuración. */
fun arbInput(name: String): Provider<String> =
    providers.gradleProperty(name).orElse(providers.environmentVariable(name)).map { it.trim() }.filter { it.isNotEmpty() }

/** Ruta de entrada; las relativas se resuelven desde la raíz del repo. */
fun arbFile(name: String): Provider<File> {
    val root = rootProject.projectDir // local: la lambda no puede capturar el objeto script (caché de configuración)
    return arbInput(name).map { path -> File(path).let { if (it.isAbsolute) it else root.resolve(path) } }
}

android {
    namespace = "dev.lluissunol.arbitratge"
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }

    defaultConfig {
        // applicationId definitiu (decidit 2026-11-20, 05 §7): no es pot canviar un cop publicat.
        applicationId = "dev.lluissunol.arbitratge"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    // 03 §10.5: catalán único (recursos por defecto = ca).
    androidResources {
        localeFilters += "ca"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Banco de muestra y clave `dev` como entradas declaradas de los tests (nunca el banco real).
            it.inputs.file(rootProject.layout.projectDirectory.file("bank-sample/bank-sample.json"))
            it.inputs.dir(layout.projectDirectory.dir("src/debug/seal"))
        }
    }
}

kotlin {
    jvmToolchain(17)
}

ksp {
    // Esquemas Room versionados (MigrationTestHelper, 03 §4.4 regla 3).
    arg("room.schemaLocation", "$projectDir/schemas")
}

androidComponents {
    onVariants { variant ->
        val isRelease = variant.buildType == "release"
        val cap = variant.name.replaceFirstChar { it.uppercase() }
        val rootDir = rootProject.layout.projectDirectory
        val buildCacheOn = gradle.startParameter.isBuildCacheEnabled

        val packBank = tasks.register<PackBankTask>("pack${cap}Bank") {
            group = "arbitratge"
            description = "Valida y copia el banco a assets/bank.json (${variant.name}), sin `canary`."
            toolClasspath.from(buildTools)
            release.set(isRelease)
            buildCacheEnabled.set(buildCacheOn)
            if (isRelease) {
                bankFile.set(layout.file(arbFile("ARB_BANK_FILE")))
            } else {
                // Debug SIEMPRE usa bank-sample: ARB_BANK_FILE se ignora (el banco real no entra en APK de debug).
                bankFile.set(rootDir.file("bank-sample/bank-sample.json"))
                if (arbInput("ARB_BANK_FILE").isPresent) {
                    logger.warn("AVÍS: ARB_BANK_FILE s'ignora en debug; es fa servir bank-sample.")
                }
            }
            outputDir.set(layout.buildDirectory.dir("generated/arbitratge/${variant.name}/bank"))
        }
        variant.sources.assets?.addGeneratedSourceDirectory(packBank, PackBankTask::outputDir)

        val packSeal = tasks.register<PackSealKeyTask>("pack${cap}SealKey") {
            group = "arbitratge"
            description = "Valida e incorpora la clave de sello (${variant.name}) como assets/seal/."
            toolClasspath.from(buildTools)
            release.set(isRelease)
            buildCacheEnabled.set(buildCacheOn)
            denyFiles.from(rootDir.file("test-vectors/test-keys.json"), layout.projectDirectory.file("src/debug/seal/dev-seal-key.der"))
            if (isRelease) {
                keyFile.set(layout.file(arbFile("ARB_SEAL_KEY_FILE")))
                kid.set(arbInput("ARB_SEAL_KID"))
            } else {
                // La clave de debug es fija: no se admite sustituirla por una de producción.
                keyFile.set(layout.projectDirectory.file("src/debug/seal/dev-seal-key.der"))
                kid.set("dev")
            }
            outputDir.set(layout.buildDirectory.dir("generated/arbitratge/${variant.name}/seal"))
        }
        variant.sources.assets?.addGeneratedSourceDirectory(packSeal, PackSealKeyTask::outputDir)
    }
}

dependencies {
    implementation(project(":core-exam"))
    implementation(project(":core-credential"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coroutines.android)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.zxing.core)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
}
