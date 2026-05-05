package es.revilofe.prog.u09.ejemplos

import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `ConexionValida`.
 *
 * Muestra cómo abrir una conexión JDBC directa, comprobar que responde con `isValid` y ver la
 * URL usada por la base de datos de demostración.
 *
 * Es un `object` porque solo actúa como lanzador del ejemplo. Kotlin lo compila como singleton,
 * evitando crear una clase instanciable sin estado.
 */
object ConexionValidaSimple {
    /**
     * Punto de entrada usado por Gradle/JVM para ejecutar el ejemplo.
     *
     * `@JvmStatic` fuerza la generación de un método `main` estático dentro del bytecode. Sin
     * esa anotación, el `main` viviría como método de la instancia singleton y algunas tareas de
     * ejecución Java no lo localizarían como entrada clásica.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("ConexionValida_simple")

        DemoDatabase.newConnection().use { connection ->
            println("URL JDBC: ${DemoDatabase.jdbcUrl}")
            println("Conexión válida: ${connection.isValid(2)}")
        }
    }
}

/**
 * Ejecuta la versión completa del ejemplo `ConexionValida`.
 *
 * Aporta una pequeña clase de servicio que encapsula la comprobación de salud y devuelve un
 * informe, en lugar de mezclar toda la lógica en el `main` simple.
 *
 * Es un singleton lanzable: no guarda estado propio y solo coordina los objetos del ejemplo.
 */
object ConexionValidaCompleto {
    /**
     * Punto de entrada estático generado para que la herramienta de ejecución pueda invocarlo.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("ConexionValida_completo")

        val checker = ConnectionHealthChecker(DemoDatabase.dataSource())
        val report = checker.check()

        println("URL JDBC: ${report.jdbcUrl}")
        println("Conexión válida: ${report.esValida}")
        println("Productos detectados en arranque: ${report.totalProductos}")
    }
}

/**
 * Resume el estado mínimo de una conexión verificada.
 *
 * @property jdbcUrl URL utilizada en la comprobación.
 * @property esValida indica si la conexión responde correctamente.
 * @property totalProductos recuento simple para confirmar que el esquema está accesible.
 */
private data class ConnectionReport(
    val jdbcUrl: String,
    val esValida: Boolean,
    val totalProductos: Int
)

/**
 * Encapsula la verificación de salud de la conexión.
 *
 * @property dataSource origen de datos contra el que se realiza la comprobación.
 */
private class ConnectionHealthChecker(
    private val dataSource: DataSource
) {
    /**
     * Abre una conexión, comprueba su validez y confirma acceso a la tabla `productos`.
     *
     * @return informe mínimo de conexión.
     */
    fun check(): ConnectionReport {
        dataSource.connection.use { connection ->
            val totalProductos = connection.prepareStatement(
                "SELECT COUNT(*) AS total FROM productos"
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    resultSet.getInt("total")
                }
            }

            return ConnectionReport(
                jdbcUrl = DemoDatabase.jdbcUrl,
                esValida = connection.isValid(2),
                totalProductos = totalProductos
            )
        }
    }
}
