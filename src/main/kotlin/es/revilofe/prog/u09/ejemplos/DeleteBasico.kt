package es.revilofe.prog.u09.ejemplos

import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `DeleteBasico`.
 *
 * Muestra cómo crear un registro temporal y borrarlo después con una sentencia `DELETE`
 * parametrizada.
 *
 * Se declara como `object` porque es un lanzador singleton sin estado propio.
 */
object DeleteBasicoSimple {
    /**
     * Punto de entrada del ejemplo.
     *
     * `@JvmStatic` genera el `main` estático que la JVM sabe invocar directamente.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("DeleteBasico_simple")

        DemoDatabase.newConnection().use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO productos (id, nombre, precio, stock, categoria_id)
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setLong(1, 99L)
                statement.setString(2, "Producto temporal")
                statement.setBigDecimal(3, java.math.BigDecimal("9.99"))
                statement.setInt(4, 1)
                statement.setLong(5, 1L)
                statement.executeUpdate()
            }

            connection.prepareStatement("DELETE FROM productos WHERE id = ?").use { statement ->
                statement.setLong(1, 99L)
                println("Filas borradas: ${statement.executeUpdate()}")
            }
        }
    }
}

/**
 * Ejecuta la versión completa del ejemplo `DeleteBasico`.
 *
 * Aporta un servicio de borrado que comprueba el resultado de `executeUpdate` y un repositorio
 * que separa creación temporal, borrado y lectura de verificación.
 *
 * El `object` solo coordina la ejecución de servicio y repositorio.
 */
object DeleteBasicoCompleto {
    /**
     * Punto de entrada estático de la versión completa.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("DeleteBasico_completo")

        val service = ProductDeletionService(ProductDeletionRepository(DemoDatabase.dataSource()))
        service.createDisposableProduct()
        service.delete(99L)

        println("El producto 99 ya no existe: ${service.find(99L) == null}")
    }
}

/**
 * Servicio de aplicación para borrados seguros.
 *
 * @property repository repositorio responsable de borrar productos.
 */
private class ProductDeletionService(
    private val repository: ProductDeletionRepository
) {
    /**
     * Crea un producto temporal para poder demostrar un borrado correcto.
     */
    fun createDisposableProduct() {
        repository.insertDisposableProduct()
    }

    /**
     * Elimina el producto indicado comprobando que exista.
     *
     * @param productId identificador del producto a eliminar.
     */
    fun delete(productId: Long) {
        val deletedRows = repository.deleteById(productId)
        // En un borrado por clave primaria esperamos exactamente una fila. Cualquier otro valor
        // revela que el identificador no existía o que la consulta no era suficientemente precisa.
        require(deletedRows == 1) { "No existe el producto con id $productId." }
    }

    /**
     * Recupera el producto indicado tras el borrado para comprobar el resultado.
     *
     * @param productId identificador del producto.
     * @return producto encontrado o `null`.
     */
    fun find(productId: Long): ProductView? = repository.findById(productId)
}

/**
 * Repositorio JDBC de borrado y lectura.
 *
 * @property dataSource origen de datos de la aplicación.
 */
private class ProductDeletionRepository(
    private val dataSource: DataSource
) {
    /**
     * Inserta un producto sin referencias para demostrar un borrado limpio.
     *
     * No se borra un producto semilla porque algunos tienen relaciones con pedidos. Así se evita
     * mezclar el objetivo del ejemplo con errores de integridad referencial.
     */
    fun insertDisposableProduct() {
        val sql = """
            INSERT INTO productos (id, nombre, precio, stock, categoria_id)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, 99L)
                statement.setString(2, "Producto temporal")
                statement.setBigDecimal(3, java.math.BigDecimal("9.99"))
                statement.setInt(4, 1)
                statement.setLong(5, 1L)
                statement.executeUpdate()
            }
        }
    }

    /**
     * Borra un producto por identificador.
     *
     * @param productId identificador del producto.
     * @return número de filas afectadas.
     */
    fun deleteById(productId: Long): Int {
        dataSource.connection.use { connection ->
            connection.prepareStatement("DELETE FROM productos WHERE id = ?").use { statement ->
                statement.setLong(1, productId)
                return statement.executeUpdate()
            }
        }
    }

    /**
     * Busca un producto por identificador.
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
