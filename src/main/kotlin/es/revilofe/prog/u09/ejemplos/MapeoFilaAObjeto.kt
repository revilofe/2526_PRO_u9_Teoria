package es.revilofe.prog.u09.ejemplos

import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `MapeoFilaAObjeto`.
 */
object MapeoFilaAObjetoSimple {
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("MapeoFilaAObjeto_simple")

        DemoDatabase.newConnection().use { connection ->
            connection.prepareStatement(
                """
                SELECT p.id, p.nombre, c.nombre AS categoria, p.precio, p.stock
                FROM productos p
                JOIN categorias c ON c.id = p.categoria_id
                WHERE p.id = ?
                """.trimIndent()
            ).use { statement ->
                statement.setLong(1, 1L)

                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        val product = ProductView(
                            id = resultSet.getLong("id"),
                            nombre = resultSet.getString("nombre"),
                            categoria = resultSet.getString("categoria"),
                            precio = resultSet.getBigDecimal("precio"),
                            stock = resultSet.getInt("stock")
                        )
                        println(product)
                    }
                }
            }
        }
    }
}

/**
 * Ejecuta la versión completa del ejemplo `MapeoFilaAObjeto`.
 */
object MapeoFilaAObjetoCompleto {
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("MapeoFilaAObjeto_completo")

        val mapper = ProductRowMapper()
        val repository = MappedProductRepository(DemoDatabase.dataSource(), mapper)

        println(repository.findById(1L))
    }
}

/**
 * Convierte la fila actual de un `ResultSet` en un `ProductView`.
 */
private class ProductRowMapper {
    /**
     * Lee la fila actual y crea el objeto de dominio.
     *
     * @param resultSet resultado posicionado en una fila válida.
     * @return objeto mapeado desde la fila actual.
     */
    fun map(resultSet: java.sql.ResultSet): ProductView =
        ProductView(
            id = resultSet.getLong("id"),
            nombre = resultSet.getString("nombre"),
            categoria = resultSet.getString("categoria"),
            precio = resultSet.getBigDecimal("precio"),
            stock = resultSet.getInt("stock")
        )
}

/**
 * Repositorio que delega el mapeo en un componente específico.
 *
 * @property dataSource origen de datos del catálogo.
 * @property mapper componente que transforma filas en objetos.
 */
private class MappedProductRepository(
    private val dataSource: DataSource,
    private val mapper: ProductRowMapper
) {
    /**
     * Recupera un producto por identificador.
     *
     * @param productId identificador buscado.
     * @return producto encontrado o `null`.
     */
    fun findById(productId: Long): ProductView? {
        val sql = """
            SELECT p.id, p.nombre, c.nombre AS categoria, p.precio, p.stock
            FROM productos p
            JOIN categorias c ON c.id = p.categoria_id
            WHERE p.id = ?
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, productId)

                statement.executeQuery().use { resultSet ->
                    return if (resultSet.next()) mapper.map(resultSet) else null
                }
            }
        }
    }
}
