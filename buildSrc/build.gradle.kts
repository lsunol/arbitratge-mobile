// Tareas de build de app-candidate (packBank / packSealKey). En buildSrc para que sean clases
// estáticas serializables por la caché de configuración (las clases de un script no lo son).
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}
