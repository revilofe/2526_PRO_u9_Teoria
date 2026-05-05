package es.revilofe.prog.u09.ejemplos

import java.math.BigDecimal
import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `InsertBasico`.
 *
 * Muestra una inserción directa con `PreparedStatement`, asignando cada columna mediante
 * parámetros y comprobando las filas insertadas.
 *
 * Se usa `object` como lanzador único y sin estado.
 */
object InsertBasicoSimple {
    /**
     * Punto de entrada del ejemplo.
     *
     * `@JvmStatic` es necesario para exponer un `main` estático compatible con la ejecución JVM.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("InsertBasico_simple")

        DemoDatabase.newConnection().use { connection ->
            val sql = """
                INSERT INTO productos (id, nombre, precio, stock, categoria_id)
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent()

            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, 10L)
                statement.setString(2, "Hub USB-C")
                statement.setBigDecimal(3, BigDecimal("29.90"))
                statement.setInt(4, 15)
                statement.setLong(5, 1L)

                println("Filas insertadas: ${statement.executeUpdate()}")
            }
        }
    }
}

/**
 * Ejecuta la versión completa del ejemplo `InsertBasico`.
 *
 * Aporta un servicio con validaciones de negocio, un repositorio de escritura y una lectura
 * posterior para devolver el producto insertado como vista completa.
 *
 * El `object` solo compone servicio y repositorio para la demostración.
 */
object InsertBasicoCompleto {
    /**
     * Punto de entrada estático de la versión completa.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("InsertBasico_completo")

        val service = ProductCreationService(ProductWriterRepository(DemoDatabase.dataSource()))
        val created = service.create(
            NewProduct(
                id = 10L,
                nombre = "Hub USB-C",
                precio = BigDecimal("29.90"),
                stock = 15,
                categoriaId = 1L
            )
        )

        println("Producto insertado: $created")
    }
}

/**
 * Representa los datos necesarios para insertar un producto nuevo.
 *
 * @property id identificador que usaremos en el ejemplo.
 * @property nombre nombre comercial del producto.
 * @property precio precio del producto.
 * @property stock unidades iniciales.
 * @property categoriaId categoría a la que pertenece.
 */
private data class NewProduct(
    val id: Long,
    val nombre: String,
    val precio: BigDecimal,
    val stock: Int,
    val categoriaId: Long
)

/**
 * Servicio de aplicación para crear productos con validación previa.
 *
 * @property repository repositorio responsable de la escritura real.
 */
private class ProductCreationService(
    private val repository: ProductWriterRepository
) {
    /**
     * Valida y persiste un producto nuevo.
     *
     * @param newProduct producto recibido desde la capa cliente.
     * @return vista completa del producto ya insertado.
     */
    fun create(newProduct: NewProduct): ProductView {
        // Estas validaciones pertenecen al servicio porque expresan reglas de negocio antes de
        // llegar a la base de datos. Las restricciones SQL siguen siendo una segunda barrera.
        require(newProduct.nombre.isNotBlank()) { "El nombre no puede estar vacío." }
        require(newProduct.precio >= BigDecimal.ZERO) { "El precio no puede ser negativo." }
        require(newProduct.stock >= 0) { "El stock no puede ser negativo." }

        repository.insert(newProduct)
        // Se vuelve a consultar para devolver la misma vista que usaría una lectura normal, ya con
        // la categoría resuelta mediante JOIN.
        return repository.findById(newProduct.id) ?: error("No se ha encontrado el producto insertado.")
    }
}

/**
 * Repositorio JDBC para inserciones de producto.
 *
 * @property dataSource origen de datos de la aplicación.
 */
private class ProductWriterRepository(
    private val dataSource: DataSource
) {
    /**
     * Inserta un producto en la base de datos.
     *
     * @param newProduct datos del producto a crear.
     */
    fun insert(newProduct: NewProduct) {
        val sql = """
            INSERT INTO productos (id, nombre, precio, stock, categoria_id)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                // El orden de los `setXxx` debe coincidir con el orden de los `?` de la sentencia.
                statement.setLong(1, newProduct.id)
                statement.setString(2, newProduct.nombre)
                statement.setBigDecimal(3, newProduct.precio)
                statement.setInt(4, newProduct.stock)
                statement.setLong(5, newProduct.categoriaId)
                statement.executeUpdate()
            }
        }
    }

    /**
     * Recupera un producto por identificador.
     *
     * @param id identificador del producto buscado.
     * @return producto encontrado o `null`.
     */
    fun findById(id: Long): ProductView? {
        val sql = """
            SELECT p.id, p.nombre, c.nombre AS categoria, p.precio, p.stock
            FROM productos p
            JOIN categorias c ON c.id = p.categoria_id
            WHERE p.id = ?
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, id)

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
