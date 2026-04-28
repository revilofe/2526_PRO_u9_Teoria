package es.revilofe.prog.u09.ejemplos

import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `SelectBasico`.
 */
object SelectBasicoSimple {
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("SelectBasico_simple")

        DemoDatabase.newConnection().use { connection ->
            connection.prepareStatement(
                "SELECT id, nombre, precio FROM productos ORDER BY id"
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        println(
                            "${resultSet.getLong("id")} - " +
                                "${resultSet.getString("nombre")} - " +
                                "${resultSet.getBigDecimal("precio")} EUR"
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ejecuta la versión completa del ejemplo `SelectBasico`.
 */
object SelectBasicoCompleto {
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("SelectBasico_completo")

        val repository = ProductReaderRepository(DemoDatabase.dataSource())
        repository.findAll().forEach { product ->
            println("${product.id} - ${product.nombre} - ${product.categoria} - ${product.precio} EUR")
        }
    }
}

/**
 * Lee el catálogo de productos con una consulta de solo lectura.
 *
 * @property dataSource origen de datos JDBC.
 */
private class ProductReaderRepository(
    private val dataSource: DataSource
) {
    /**
     * Recupera todos los productos ordenados por identificador.
     *
     * @return lista de productos mapeados a objetos Kotlin.
     */
    fun findAll(): List<ProductView> {
        val sql = """
            SELECT p.id, p.nombre, c.nombre AS categoria, p.precio, p.stock
            FROM productos p
            JOIN categorias c ON c.id = p.categoria_id
            ORDER BY p.id
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val products = mutableListOf<ProductView>()

                    while (resultSet.next()) {
                        products += ProductView(
                            id = resultSet.getLong("id"),
                            nombre = resultSet.getString("nombre"),
                            categoria = resultSet.getString("categoria"),
                            precio = resultSet.getBigDecimal("precio"),
                            stock = resultSet.getInt("stock")
                        )
                    }

                    return products
                }
            }
        }
    }
}
