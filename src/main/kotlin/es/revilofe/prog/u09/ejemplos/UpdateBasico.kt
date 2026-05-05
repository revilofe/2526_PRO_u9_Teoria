package es.revilofe.prog.u09.ejemplos

import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `UpdateBasico`.
 *
 * Practica una actualización sencilla de stock con parámetros y revisión del número de filas
 * afectadas por `executeUpdate`.
 *
 * El `object` se usa como singleton lanzable para no crear objetos sin estado real.
 */
object UpdateBasicoSimple {
    /**
     * Punto de entrada del ejemplo.
     *
     * `@JvmStatic` adapta la función a la forma que esperan las herramientas Java.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("UpdateBasico_simple")

        DemoDatabase.newConnection().use { connection ->
            connection.prepareStatement(
                "UPDATE productos SET stock = ? WHERE id = ?"
            ).use { statement ->
                statement.setInt(1, 20)
                statement.setLong(2, 2L)
                println("Filas actualizadas: ${statement.executeUpdate()}")
            }
        }
    }
}

/**
 * Ejecuta la versión completa del ejemplo `UpdateBasico`.
 *
 * Aporta un servicio que valida el nuevo stock, comprueba que exista exactamente una fila
 * modificada y devuelve el producto actualizado desde un repositorio.
 *
 * El singleton agrupa únicamente la ejecución de la demo.
 */
object UpdateBasicoCompleto {
    /**
     * Punto de entrada estático de la versión completa.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("UpdateBasico_completo")

        val service = StockUpdateService(ProductStockRepository(DemoDatabase.dataSource()))
        val updated = service.changeStock(productId = 2L, newStock = 20)

        println("Producto actualizado: $updated")
    }
}

/**
 * Servicio que actualiza stock respetando validaciones de negocio.
 *
 * @property repository repositorio JDBC del stock.
 */
private class StockUpdateService(
    private val repository: ProductStockRepository
) {
    /**
     * Cambia el stock de un producto y devuelve su estado actualizado.
     *
     * @param productId producto a modificar.
     * @param newStock nuevo stock deseado.
     * @return vista completa del producto ya actualizado.
     */
    fun changeStock(productId: Long, newStock: Int): ProductView {
        require(newStock >= 0) { "El stock no puede ser negativo." }

        val updatedRows = repository.updateStock(productId, newStock)
        // `executeUpdate` devuelve cuántas filas cambian. Comprobarlo convierte un fallo silencioso
        // de SQL en un error claro del caso de uso.
        require(updatedRows == 1) { "No existe el producto con id $productId." }

        return repository.findById(productId) ?: error("No se ha encontrado el producto tras actualizarlo.")
    }
}

/**
 * Repositorio que concentra actualizaciones de stock.
 *
 * @property dataSource origen de datos del catálogo.
 */
private class ProductStockRepository(
    private val dataSource: DataSource
) {
    /**
     * Actualiza el stock del producto indicado.
     *
     * @param productId identificador del producto.
     * @param newStock stock nuevo que se quiere persistir.
     * @return número de filas afectadas.
     */
    fun updateStock(productId: Long, newStock: Int): Int {
        val sql = "UPDATE productos SET stock = ? WHERE id = ?"

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                // Primero se asignan los nuevos valores y después la clave del `WHERE`.
                statement.setInt(1, newStock)
                statement.setLong(2, productId)
                return statement.executeUpdate()
            }
        }
    }

    /**
     * Busca un producto por su identificador.
     *
     * @param productId identificador del producto.
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
                    return if (resultSet.next()) {
                        ProductView(
                            id = resultSet.getLong("id"),
                            nombre = resultSet.getString("nombre"),
                            categoria = resultSet.getString("categoria"),
                            precio = resultSet.getBigDecimal("precio"),
                            stock = resultSet.getInt("stock")
                        )
                    } else {
                        null
                    }
                }
            }
        }
    }
}
