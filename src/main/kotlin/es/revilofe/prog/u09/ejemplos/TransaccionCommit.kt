package es.revilofe.prog.u09.ejemplos

import java.math.BigDecimal
import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `TransaccionCommit`.
 */
object TransaccionCommitSimple {
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("TransaccionCommit_simple")

        DemoDatabase.newConnection().use { connection ->
            connection.autoCommit = false

            try {
                insertOrder(connection, 20L, 3L, BigDecimal("39.95"))
                reduceStock(connection, 2L, 1)
                connection.commit()
                println("Transacción confirmada correctamente.")
            } catch (exception: Exception) {
                connection.rollback()
                throw exception
            } finally {
                connection.autoCommit = true
            }
        }
    }

    private fun insertOrder(connection: java.sql.Connection, orderId: Long, customerId: Long, total: BigDecimal) {
        connection.prepareStatement(
            "INSERT INTO pedidos (id, cliente_id, fecha, total) VALUES (?, ?, CURRENT_DATE, ?)"
        ).use { statement ->
            statement.setLong(1, orderId)
            statement.setLong(2, customerId)
            statement.setBigDecimal(3, total)
            statement.executeUpdate()
        }
    }

    private fun reduceStock(connection: java.sql.Connection, productId: Long, units: Int) {
        connection.prepareStatement(
            "UPDATE productos SET stock = stock - ? WHERE id = ?"
        ).use { statement ->
            statement.setInt(1, units)
            statement.setLong(2, productId)
            statement.executeUpdate()
        }
    }
}

/**
 * Ejecuta la versión completa del ejemplo `TransaccionCommit`.
 */
object TransaccionCommitCompleto {
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("TransaccionCommit_completo")

        val service = OrderPlacementService(DemoDatabase.dataSource())
        service.registerSimpleOrder(orderId = 20L, customerId = 3L, productId = 2L, units = 1)
        println("Pedido y stock actualizados dentro de la misma transacción.")
    }
}

/**
 * Servicio que agrupa varias operaciones bajo una única transacción.
 *
 * @property dataSource origen de datos transaccional.
 */
private class OrderPlacementService(
    private val dataSource: DataSource
) {
    /**
     * Inserta un pedido y descuenta stock en una sola unidad de trabajo.
     *
     * @param orderId identificador del pedido a crear.
     * @param customerId cliente al que pertenece el pedido.
     * @param productId producto vendido.
     * @param units unidades que se descuentan.
     */
    fun registerSimpleOrder(orderId: Long, customerId: Long, productId: Long, units: Int) {
        dataSource.connection.use { connection ->
            connection.autoCommit = false

            try {
                connection.prepareStatement(
                    "INSERT INTO pedidos (id, cliente_id, fecha, total) VALUES (?, ?, CURRENT_DATE, ?)"
                ).use { statement ->
                    statement.setLong(1, orderId)
                    statement.setLong(2, customerId)
                    statement.setBigDecimal(3, BigDecimal("39.95"))
                    statement.executeUpdate()
                }

                connection.prepareStatement(
                    "UPDATE productos SET stock = stock - ? WHERE id = ?"
                ).use { statement ->
                    statement.setInt(1, units)
                    statement.setLong(2, productId)
                    statement.executeUpdate()
                }

                connection.commit()
            } catch (exception: Exception) {
                connection.rollback()
                throw exception
            } finally {
                connection.autoCommit = true
            }
        }
    }
}
