package es.revilofe.prog.u09.ejemplos

import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `SelectBasico`.
 *
 * Practica una consulta `SELECT` básica con `PreparedStatement`, recorrido de `ResultSet` y
 * escritura directa por consola.
 *
 * Se declara como `object` para representar un único lanzador sin estado. Es una forma cómoda
 * de agrupar un `main` didáctico sin diseñar una clase con constructor.
 */
object SelectBasicoSimple {
    /**
     * Punto de entrada del ejemplo.
     *
     * `@JvmStatic` adapta el `main` de Kotlin al formato estático que espera la JVM cuando se
     * lanza una clase desde Gradle o desde un IDE.
     */
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
 *
 * Aporta un repositorio que concentra el SQL y devuelve objetos `ProductView`, separando la
 * lectura de datos de la presentación por consola.
 *
 * El `object` funciona como singleton lanzador y no como entidad del dominio.
 */
object SelectBasicoCompleto {
    /**
     * Punto de entrada estático usado para ejecutar la versión con repositorio.
     */
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
