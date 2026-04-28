package es.revilofe.prog.u09.ejemplos

import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `ConexionValida`.
 */
object ConexionValidaSimple {
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
 */
object ConexionValidaCompleto {
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
