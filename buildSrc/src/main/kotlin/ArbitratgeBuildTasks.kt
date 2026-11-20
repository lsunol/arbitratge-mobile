import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

private const val TOOL = "dev.lluissunol.arbitratge.buildtools.BuildTools"

private fun requireNoBuildCache(enabled: Boolean) {
    if (enabled) {
        throw GradleException(
            "Release: cal executar amb --no-build-cache (el banc i la clau reals no poden acabar a la caché de build; 03 §5.5).",
        )
    }
}

/** `packBank` (03 §5.4): valida con el validador de `core-exam` y copia el banco sin `canary`. */
@DisableCachingByDefault(because = "Puede procesar el banco real: no debe guardarse en la caché de build")
abstract class PackBankTask @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:Classpath
    abstract val toolClasspath: ConfigurableFileCollection

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val bankFile: RegularFileProperty

    @get:Input
    abstract val release: Property<Boolean>

    /** `gradle.startParameter.isBuildCacheEnabled` al configurar. */
    @get:Input
    abstract val buildCacheEnabled: Property<Boolean>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun pack() {
        val release = release.get()
        if (release) requireNoBuildCache(buildCacheEnabled.get())
        if (!bankFile.isPresent) {
            throw GradleException(
                if (release) "Release: cal ARB_BANK_FILE (variable d'entorn o propietat Gradle) amb el banc real."
                else "No s'ha trobat el banc de mostra.",
            )
        }
        val out = outputDir.get().asFile.apply { deleteRecursively(); mkdirs() }
        exec.javaexec {
            classpath = toolClasspath
            mainClass.set(TOOL)
            args("pack-bank", bankFile.get().asFile.absolutePath, File(out, "bank.json").absolutePath, if (release) "release" else "debug")
        }
    }
}

/** `packSealKey` (03 §6.4, 05 §4.3.1): valida kid y PKCS#8 P-256 y los incorpora como `assets/seal/`. */
@DisableCachingByDefault(because = "Manipula material de clave de sello")
abstract class PackSealKeyTask @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:Classpath
    abstract val toolClasspath: ConfigurableFileCollection

    @get:Optional
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val keyFile: RegularFileProperty

    @get:Optional
    @get:Input
    abstract val kid: Property<String>

    @get:Input
    abstract val release: Property<Boolean>

    @get:Input
    abstract val buildCacheEnabled: Property<Boolean>

    /** Claves de debug/prueba que un release no puede embeber (`.der` o `test-keys.json`). */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val denyFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun pack() {
        val release = release.get()
        if (release) requireNoBuildCache(buildCacheEnabled.get())
        if (!keyFile.isPresent || !kid.isPresent) {
            throw GradleException(
                if (release) "Release: calen ARB_SEAL_KEY_FILE (PKCS#8 DER P-256) i ARB_SEAL_KID (variable d'entorn o propietat Gradle)."
                else "Falta la clau de sello de debug (src/debug/seal).",
            )
        }
        val out = outputDir.get().asFile.apply { deleteRecursively(); mkdirs() }
        exec.javaexec {
            classpath = toolClasspath
            mainClass.set(TOOL)
            args("pack-seal", keyFile.get().asFile.absolutePath, kid.get(), File(out, "seal").absolutePath, if (release) "release" else "debug", *denyFiles.files.map { it.absolutePath }.toTypedArray())
        }
    }
}
