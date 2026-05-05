package es.revilofe.prog.u09.ejemplos

import java.time.LocalDate
import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `MapeoUnoAMuchos`.
 *
 * Muestra cómo una relación cliente-pedidos aparece como varias filas tras un `JOIN`, repitiendo
 * los datos del cliente por cada pedido.
 *
 * Es un `object` porque solo contiene el lanzador del caso simple.
 */
object MapeoUnoAMuchosSimple {
    /**
     * Punto de entrada del ejemplo.
     *
     * `@JvmStatic` expone esta función con la forma estática que espera la JVM.
     */
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
                        // En una relación uno a muchos, una consulta con JOIN repite los datos del
                        // cliente en cada fila. La versión simple imprime filas; la completa
                        // reconstruye un objeto con una lista de pedidos.
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
 *
 * Aporta la reconstrucción de un agregado `CustomerWithOrders`: crea el cliente una vez, acumula
 * pedidos y contempla clientes sin pedidos mediante `LEFT JOIN`.
 *
 * El singleton solo arranca la demo y no conserva estado entre ejecuciones.
 */
object MapeoUnoAMuchosCompleto {
    /**
     * Punto de entrada estático de la versión completa.
     */
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
                            // Los datos del cliente aparecen repetidos en todas las filas del JOIN.
                            // Lo creamos una sola vez y luego acumulamos los pedidos.
                            customer = Customer(
                                id = resultSet.getLong("cliente_id"),
                                nombre = resultSet.getString("nombre"),
                                email = resultSet.getString("email")
                            )
                        }

                        val orderId = resultSet.getLong("pedido_id")
                        if (!resultSet.wasNull()) {
                            // Con `LEFT JOIN` puede no haber pedido. Tras leer un Long, JDBC devuelve
                            // 0 si la columna era NULL, por eso se comprueba `wasNull()`.
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
