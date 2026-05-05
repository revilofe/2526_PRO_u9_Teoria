import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSetContainer

plugins {
    kotlin("jvm") version "1.9.24"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.h2database:h2:2.2.224")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.slf4j:slf4j-simple:2.0.13")
}

kotlin {
    jvmToolchain(11)
}

val exampleNames = listOf(
    "ConexionValida",
    "SelectBasico",
    "StatementSoloLectura",
    "PreparedSelectParametro",
    "InsertBasico",
    "UpdateBasico",
    "DeleteBasico",
    "MapeoFilaAObjeto",
    "MapeoUnoAMuchos",
    "GestionSQLException",
    "CierreRecursosUse",
    "TransaccionCommit",
    "TransaccionRollback",
    "DaoBasico",
    "DaoConServicio",
    "PoolHikariBasico"
)

val mainRuntimeClasspath = the<SourceSetContainer>()["main"].runtimeClasspath

fun registerExampleTask(taskName: String, mainClassName: String) {
    tasks.register<JavaExec>(taskName) {
        group = "application"
        description = "Ejecuta $mainClassName"
        classpath = mainRuntimeClasspath
        mainClass.set(mainClassName)
        dependsOn(tasks.named("classes"))
    }
}

exampleNames.forEach { exampleName ->
    registerExampleTask(
        taskName = "run${exampleName}_simple",
        mainClassName = "es.revilofe.prog.u09.ejemplos.${exampleName}Simple"
    )
    registerExampleTask(
        taskName = "run${exampleName}_completo",
        mainClassName = "es.revilofe.prog.u09.ejemplos.${exampleName}Completo"
    )
}

tasks.register("listarEjemplos") {
    group = "help"
    description = "Muestra todas las tareas de ejemplo disponibles."
    doLast {
        println("Ejemplos disponibles:")
        exampleNames.forEach { exampleName ->
            println(" - run${exampleName}_simple")
            println(" - run${exampleName}_completo")
        }
    }
}
