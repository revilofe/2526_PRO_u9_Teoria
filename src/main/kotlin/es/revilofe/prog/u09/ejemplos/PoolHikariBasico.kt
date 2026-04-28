package es.revilofe.prog.u09.ejemplos

import com.zaxxer.hikari.HikariDataSource

/**
 * Ejecuta la versión simple del ejemplo `PoolHikariBasico`.
 */
object PoolHikariBasicoSimple {
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("PoolHikariBasico_simple")

        DemoDatabase.hikariDataSource(poolName = "SimplePool", maxPoolSize = 2).use { dataSource ->
            dataSource.connection.use { connection ->
                println("Conexión obtenida del pool: ${connection.isValid(2)}")
            }
        }
    }
}

/**
 * Ejecuta la versión completa del ejemplo `PoolHikariBasico`.
 */
object PoolHikariBasicoCompleto {
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
            connection.prepareStatement("SELECT COUNT(*) AS total FROM productos").use { statement ->
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    return resultSet.getInt("total")
                }
            }
        }
    }
}
