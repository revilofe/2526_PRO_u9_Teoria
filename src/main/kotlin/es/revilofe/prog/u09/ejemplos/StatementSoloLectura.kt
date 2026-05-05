package es.revilofe.prog.u09.ejemplos

import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `StatementSoloLectura`.
 *
 * Muestra cuándo puede usarse `Statement`: una consulta fija, sin parámetros externos, orientada
 * solo a lectura.
 *
 * El `object` evita instanciar una clase que solo existe para contener el `main`.
 */
object StatementSoloLecturaSimple {
    /**
     * Punto de entrada del ejemplo.
     *
     * `@JvmStatic` genera la firma Java clásica `public static void main(String[] args)`.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("StatementSoloLectura_simple")

        DemoDatabase.newConnection().use { connection ->
            // `Statement` solo es aceptable aquí porque el SQL es fijo y no incorpora datos del
            // usuario. Si hubiese parámetros, debería usarse `PreparedStatement`.
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
 *
 * Aporta un servicio de lectura y una fila de inventario como modelo, de forma que el `main` no
 * depende de la API JDBC ni de los nombres de columnas.
 *
 * Se mantiene como singleton porque solo orquesta la ejecución del caso completo.
 */
object StatementSoloLecturaCompleto {
    /**
     * Punto de entrada estático usado por las tareas `run...`.
     */
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
            // Este ejemplo conserva `Statement` para comparar APIs. La regla práctica es sencilla:
            // SQL fijo puede usar `Statement`; SQL con valores externos debe usar parámetros.
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
