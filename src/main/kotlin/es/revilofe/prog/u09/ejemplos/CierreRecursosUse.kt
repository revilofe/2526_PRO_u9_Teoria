package es.revilofe.prog.u09.ejemplos

import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `CierreRecursosUse`.
 *
 * Practica el cierre seguro de `Connection`, `PreparedStatement` y `ResultSet` mediante llamadas
 * anidadas a `use`.
 *
 * Se usa `object` como singleton porque este lanzador no necesita estado ni constructor.
 */
object CierreRecursosUseSimple {
    /**
     * Punto de entrada del ejemplo.
     *
     * `@JvmStatic` genera una entrada estática compatible con el lanzador JVM.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("CierreRecursosUse_simple")

        DemoDatabase.newConnection().use { connection ->
            // `use` es el equivalente idiomático en Kotlin a `try-with-resources` de Java:
            // cierra la conexión aunque la consulta lance una excepción.
            connection.prepareStatement("SELECT nombre FROM clientes ORDER BY id").use { statement ->
                // Cada recurso JDBC se cierra en orden inverso: primero ResultSet, luego
                // PreparedStatement y por último Connection.
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
 *
 * Aporta un lector reutilizable que encapsula el patrón de cierre automático y devuelve una lista
 * de nombres, dejando el `main` como una capa de presentación mínima.
 *
 * El `object` contiene solo la composición mínima para arrancar la versión completa.
 */
object CierreRecursosUseCompleto {
    /**
     * Punto de entrada estático de la versión completa.
     */
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
            // Anidar `use` hace explícito el ciclo de vida de cada recurso JDBC y evita fugas de
            // conexiones, uno de los errores más frecuentes al empezar con bases de datos.
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
