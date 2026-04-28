package es.revilofe.prog.u09.ejemplos

import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `StatementSoloLectura`.
 */
object StatementSoloLecturaSimple {
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("StatementSoloLectura_simple")

        DemoDatabase.newConnection().use { connection ->
            // `Statement` solo es aceptable aquí porque el SQL es fijo.
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT nombre, stock FROM productos ORDER BY stock DESC"
                ).use { resultSet ->
                    while (resultSet.next()) {
                        println("${resultSet.getString("nombre")} -> ${resultSet.getInt("stock")} unidades")
                    }
                }
            }
        }
    }
}

/**
 * Ejecuta la versión completa del ejemplo `StatementSoloLectura`.
 */
object StatementSoloLecturaCompleto {
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("StatementSoloLectura_completo")

        val service = ReadOnlyInventoryService(DemoDatabase.dataSource())
        service.loadRows().forEach { row ->
            println("${row.nombre} -> ${row.stock} unidades")
        }
    }
}

/**
 * Representa una fila simple de inventario para lecturas de solo consulta.
 *
 * @property nombre nombre del producto.
 * @property stock cantidad disponible.
 */
private data class InventoryRow(
    val nombre: String,
    val stock: Int
)

/**
 * Servicio de lectura que encapsula el uso de `Statement` con SQL fijo.
 *
 * @property dataSource origen de datos del inventario.
 */
private class ReadOnlyInventoryService(
    private val dataSource: DataSource
) {
    /**
     * Recupera el inventario ordenado por stock.
     *
     * @return filas del inventario listas para mostrar.
     */
    fun loadRows(): List<InventoryRow> {
        val rows = mutableListOf<InventoryRow>()

        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    "SELECT nombre, stock FROM productos ORDER BY stock DESC"
                ).use { resultSet ->
                    while (resultSet.next()) {
                        rows += InventoryRow(
                            nombre = resultSet.getString("nombre"),
                            stock = resultSet.getInt("stock")
                        )
                    }
                }
            }
        }

        return rows
    }
}
