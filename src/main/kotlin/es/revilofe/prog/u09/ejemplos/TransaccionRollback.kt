package es.revilofe.prog.u09.ejemplos

import java.math.BigDecimal
import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `TransaccionRollback`.
 *
 * Fuerza un fallo después de un `INSERT` para demostrar que `rollback` deshace los cambios hechos
 * dentro de la misma conexión transaccional.
 *
 * Se declara como `object` porque solo contiene el lanzador de una demo repetible.
 */
object TransaccionRollbackSimple {
    /**
     * Punto de entrada del ejemplo.
     *
     * `@JvmStatic` adapta la función `main` de Kotlin al punto de entrada estático de Java.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("TransaccionRollback_simple")

        DemoDatabase.newConnection().use { connection ->
            // Sin esta línea, cada sentencia haría commit automáticamente y el rollback no podría
            // deshacer el INSERT ya ejecutado.
            connection.autoCommit = false

            try {
                connection.prepareStatement(
                    "INSERT INTO pedidos (id, cliente_id, fecha, total) VALUES (?, ?, CURRENT_DATE, ?)"
                ).use { statement ->
                    statement.setLong(1, 30L)
                    statement.setLong(2, 1L)
                    statement.setBigDecimal(3, BigDecimal("999.00"))
                    statement.executeUpdate()
                }

                error("Forzamos un fallo para demostrar el rollback.")
            } catch (exception: Exception) {
                connection.rollback()
                println("Se ha hecho rollback: ${exception.message}")
            } finally {
                // Es buena práctica restaurar la conexión aunque el ejemplo use una conexión nueva.
                connection.autoCommit = true
            }
        }
    }
}

/**
 * Ejecuta la versión completa del ejemplo `TransaccionRollback`.
 *
 * Aporta un servicio que cuenta pedidos antes y después del intento fallido, mostrando con datos
 * observables que la transacción no dejó cambios persistidos.
 *
 * El singleton no modela negocio; solo coordina la ejecución del caso completo.
 */
object TransaccionRollbackCompleto {
    /**
     * Punto de entrada estático de la versión completa.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("TransaccionRollback_completo")

        val service = SafeSalesService(DemoDatabase.dataSource())
        println("Pedidos antes del intento: ${service.countOrders()}")
        service.tryOperationWithRollback()
        println("Pedidos después del intento: ${service.countOrders()}")
    }
}

/**
 * Servicio de ventas que demuestra una reversión transaccional.
 *
 * @property dataSource origen de datos JDBC.
 */
private class SafeSalesService(
    private val dataSource: DataSource
) {
    /**
     * Cuenta pedidos para comprobar el efecto del rollback.
     *
     * @return número de pedidos almacenados.
     */
    fun countOrders(): Int {
        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT COUNT(*) AS total FROM pedidos").use { statement ->
                statement.executeQuery().use { resultSet ->
                    resultSet.next()
                    return resultSet.getInt("total")
                }
            }
        }
    }

    /**
     * Ejecuta una operación que siempre termina en rollback.
     */
    fun tryOperationWithRollback() {
        dataSource.connection.use { connection ->
            // El rollback solo afecta a las operaciones hechas en esta conexión desde que se
            // desactivó `autoCommit`.
            connection.autoCommit = false

            try {
                connection.prepareStatement(
                    "INSERT INTO pedidos (id, cliente_id, fecha, total) VALUES (?, ?, CURRENT_DATE, ?)"
                ).use { statement ->
                    statement.setLong(1, 30L)
                    statement.setLong(2, 1L)
                    statement.setBigDecimal(3, BigDecimal("999.00"))
                    statement.executeUpdate()
                }

                throw IllegalStateException("Forzamos un fallo para preservar la consistencia.")
            } catch (exception: Exception) {
                connection.rollback()
                println("Rollback ejecutado: ${exception.message}")
            } finally {
                // Si esta conexión viniera de un pool, se devolvería limpia para el siguiente uso.
                connection.autoCommit = true
            }
        }
    }
}
