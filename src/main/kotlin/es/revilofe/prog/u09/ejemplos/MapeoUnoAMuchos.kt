package es.revilofe.prog.u09.ejemplos

import java.time.LocalDate
import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `MapeoUnoAMuchos`.
 */
object MapeoUnoAMuchosSimple {
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("MapeoUnoAMuchos_simple")

        DemoDatabase.newConnection().use { connection ->
            connection.prepareStatement(
                """
                SELECT c.id AS cliente_id, c.nombre, c.email, p.id AS pedido_id, p.fecha, p.total
                FROM clientes c
                JOIN pedidos p ON p.cliente_id = c.id
                WHERE c.id = ?
                ORDER BY p.fecha
                """.trimIndent()
            ).use { statement ->
                statement.setLong(1, 1L)

                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        println(
                            "${resultSet.getString("nombre")} -> " +
                                "pedido ${resultSet.getLong("pedido_id")} -> " +
                                "${resultSet.getBigDecimal("total")} EUR"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ejecuta la versión completa del ejemplo `MapeoUnoAMuchos`.
 */
object MapeoUnoAMuchosCompleto {
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("MapeoUnoAMuchos_completo")

        val repository = CustomerOrdersRepository(DemoDatabase.dataSource())
        println(repository.findCustomerWithOrders(1L))
    }
}

/**
 * Agrupa el trabajo de reconstrucción de un agregado cliente-pedidos.
 *
 * @property dataSource origen de datos JDBC.
 */
private class CustomerOrdersRepository(
    private val dataSource: DataSource
) {
    /**
     * Busca un cliente y todos sus pedidos en una sola consulta.
     *
     * @param customerId cliente cuyo agregado queremos reconstruir.
     * @return cliente con sus pedidos, o `null` si no existe.
     */
    fun findCustomerWithOrders(customerId: Long): CustomerWithOrders? {
        val sql = """
            SELECT c.id AS cliente_id, c.nombre, c.email, p.id AS pedido_id, p.fecha, p.total
            FROM clientes c
            LEFT JOIN pedidos p ON p.cliente_id = c.id
            WHERE c.id = ?
            ORDER BY p.fecha
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, customerId)

                statement.executeQuery().use { resultSet ->
                    var customer: Customer? = null
                    val orders = mutableListOf<OrderSummary>()

                    while (resultSet.next()) {
                        if (customer == null) {
                            customer = Customer(
                                id = resultSet.getLong("cliente_id"),
                                nombre = resultSet.getString("nombre"),
                                email = resultSet.getString("email")
                            )
                        }

                        val orderId = resultSet.getLong("pedido_id")
                        if (!resultSet.wasNull()) {
                            orders += OrderSummary(
                                id = orderId,
                                fecha = resultSet.getDate("fecha").toLocalDate(),
                                total = resultSet.getBigDecimal("total")
                            )
                        }
                    }

                    return customer?.let { CustomerWithOrders(it, orders) }
                }
            }
        }
    }
}
