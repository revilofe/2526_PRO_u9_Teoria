package es.revilofe.prog.u09.ejemplos

import java.math.BigDecimal
import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `TransaccionCommit`.
 *
 * Muestra cómo agrupar la creación de un pedido y el descuento de stock en una transacción manual
 * que confirma los cambios con `commit`.
 *
 * El `object` actúa como singleton lanzador y mantiene el ejemplo sin estado compartido.
 */
object TransaccionCommitSimple {
    /**
     * Punto de entrada del ejemplo.
     *
     * `@JvmStatic` genera el `main` estático que necesitan las tareas de ejecución JVM.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("TransaccionCommit_simple")

        DemoDatabase.newConnection().use { connection ->
            // Desactivamos `autoCommit` para que varias sentencias formen una única unidad de
            // trabajo: o se confirman todas, o se revierten todas.
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
                // Devolver `autoCommit` a su estado habitual es importante cuando la conexión
                // procede de un pool y puede reutilizarse en otra operación.
                connection.autoCommit = true
            }
        }
    }

    /**
     * Inserta la cabecera de un pedido usando la conexión recibida.
     *
     * Recibir la conexión por parámetro es intencionado: todas las operaciones de la transacción
     * deben compartir la misma conexión JDBC para participar en el mismo `commit` o `rollback`.
     */
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

    /**
     * Descuenta stock usando la misma conexión transaccional que creó el pedido.
     */
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
 *
 * Aporta un servicio transaccional que concentra la unidad de trabajo y oculta al `main` los
 * detalles de `autoCommit`, `commit`, `rollback` y sentencias JDBC.
 *
 * El singleton solo arranca el servicio transaccional de la demostración.
 */
object TransaccionCommitCompleto {
    /**
     * Punto de entrada estático de la versión completa.
     */
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
            // En JDBC una transacción pertenece a una conexión concreta. Por eso todas las
            // sentencias de esta operación se ejecutan con el mismo objeto `connection`.
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
                // Restaurar el modo automático evita que la siguiente operación herede un estado
                // transaccional inesperado.
                connection.autoCommit = true
            }
        }
    }
}
