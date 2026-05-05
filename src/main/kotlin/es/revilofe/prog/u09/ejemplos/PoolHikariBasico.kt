package es.revilofe.prog.u09.ejemplos

import com.zaxxer.hikari.HikariDataSource

/**
 * Ejecuta la versión simple del ejemplo `PoolHikariBasico`.
 *
 * Muestra cómo crear un pool HikariCP, pedir una conexión prestada y comprobar que es válida.
 *
 * El `object` solo sirve como lanzador singleton del caso simple.
 */
object PoolHikariBasicoSimple {
    /**
     * Punto de entrada del ejemplo.
     *
     * `@JvmStatic` genera el método `main` estático esperado por la JVM.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("PoolHikariBasico_simple")

        DemoDatabase.hikariDataSource(poolName = "SimplePool", maxPoolSize = 2).use { dataSource ->
            // También cerramos el `DataSource`: en HikariCP eso libera el pool completo, no solo
            // una conexión individual.
            dataSource.connection.use { connection ->
                println("Conexión obtenida del pool: ${connection.isValid(2)}")
            }
        }
    }
}

/**
 * Ejecuta la versión completa del ejemplo `PoolHikariBasico`.
 *
 * Aporta un servicio que usa el pool para consultar datos y muestra una métrica básica del pool
 * después de devolver la conexión.
 *
 * El singleton arranca el ejemplo con pool sin guardar estado propio.
 */
object PoolHikariBasicoCompleto {
    /**
     * Punto de entrada estático de la versión completa.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("PoolHikariBasico_completo")

        DemoDatabase.hikariDataSource(poolName = "CompletePool", maxPoolSize = 3).use { dataSource ->
            val service = PooledCatalogService(dataSource)
            println("Productos en catálogo: ${service.countProducts()}")
            println("Conexiones activas tras la consulta: ${dataSource.hikariPoolMXBean.activeConnections}")
        }
    }
}

/**
 * Servicio que trabaja contra un `DataSource` con pool.
 *
 * Aunque recibe `HikariDataSource` para mostrar métricas del pool en el ejemplo, en código de
 * negocio normalmente bastaría con depender de la interfaz `DataSource`.
 *
 * @property dataSource pool HikariCP usado por el servicio.
 */
private class PooledCatalogService(
    private val dataSource: HikariDataSource
) {
    /**
     * Cuenta los productos del catálogo utilizando una conexión prestada por el pool.
     *
     * @return número total de productos.
     */
    fun countProducts(): Int {
        dataSource.connection.use { connection ->
            // Al cerrar esta conexión con `use`, HikariCP no cierra la conexión física: la devuelve
            // al pool para que pueda reutilizarse.
            connection.prepareStatement("SELECT COUNT(*) AS total FROM productos").use { statement ->
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    return resultSet.getInt("total")
                }
            }
        }
    }
}
