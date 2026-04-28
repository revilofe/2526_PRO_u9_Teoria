package es.revilofe.prog.u09.ejemplos

import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `CierreRecursosUse`.
 */
object CierreRecursosUseSimple {
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("CierreRecursosUse_simple")

        DemoDatabase.newConnection().use { connection ->
            connection.prepareStatement("SELECT nombre FROM clientes ORDER BY id").use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        println(resultSet.getString("nombre"))
                    }
                }
            }
        }
    }
}

/**
 * Ejecuta la versión completa del ejemplo `CierreRecursosUse`.
 */
object CierreRecursosUseCompleto {
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("CierreRecursosUse_completo")

        val reader = SafeCustomerReader(DemoDatabase.dataSource())
        reader.readNames().forEach(::println)
    }
}

/**
 * Componente que muestra el patrón completo de cierre automático con `use`.
 *
 * @property dataSource origen de datos JDBC.
 */
private class SafeCustomerReader(
    private val dataSource: DataSource
) {
    /**
     * Lee los nombres de cliente garantizando el cierre de todos los recursos.
     *
     * @return nombres de cliente ordenados por identificador.
     */
    fun readNames(): List<String> {
        val names = mutableListOf<String>()

        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT nombre FROM clientes ORDER BY id").use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        names += resultSet.getString("nombre")
                    }
                }
            }
        }

        return names
    }
}
