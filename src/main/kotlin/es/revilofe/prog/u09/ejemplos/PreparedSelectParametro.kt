package es.revilofe.prog.u09.ejemplos

import java.math.BigDecimal
import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `PreparedSelectParametro`.
 */
object PreparedSelectParametroSimple {
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("PreparedSelectParametro_simple")

        DemoDatabase.newConnection().use { connection ->
            val sql = """
                SELECT nombre, precio
                FROM productos
                WHERE precio <= ?
                ORDER BY precio
            """.trimIndent()

            connection.prepareStatement(sql).use { statement ->
                statement.setBigDecimal(1, BigDecimal("100.00"))

                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        println("${resultSet.getString("nombre")} -> ${resultSet.getBigDecimal("precio")} EUR")
                    }
                }
            }
        }
    }
}

/**
 * Ejecuta la versión completa del ejemplo `PreparedSelectParametro`.
 */
object PreparedSelectParametroCompleto {
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("PreparedSelectParametro_completo")

        val criteria = PriceFilter(maxPrice = BigDecimal("100.00"))
        val repository = PreparedProductSearchRepository(DemoDatabase.dataSource())

        println("Consulta insegura que no debe ejecutarse con datos externos:")
        println(repository.buildUnsafeQuery(criteria))
        println()
        println("Consulta segura ejecutada con PreparedStatement:")

        repository.findByPrice(criteria).forEach { product ->
            println("${product.nombre} -> ${product.precio} EUR")
        }
    }
}

/**
 * Modela un filtro de precio para búsquedas parametrizadas.
 *
 * @property maxPrice importe máximo aceptado en la consulta.
 */
private data class PriceFilter(
    val maxPrice: BigDecimal
)

/**
 * Repositorio de búsqueda que muestra el patrón correcto con parámetros.
 *
 * @property dataSource origen de datos del catálogo.
 */
private class PreparedProductSearchRepository(
    private val dataSource: DataSource
) {
    /**
     * Construye una consulta insegura solo para comparar estilos.
     *
     * @param criteria filtro usado en el ejemplo.
     * @return cadena SQL concatenada que no conviene ejecutar con datos externos.
     */
    fun buildUnsafeQuery(criteria: PriceFilter): String =
        "SELECT nombre, precio FROM productos WHERE precio <= '${criteria.maxPrice}' ORDER BY precio"

    /**
     * Ejecuta la consulta de forma segura con `PreparedStatement`.
     *
     * @param criteria filtro de precio a aplicar.
     * @return productos cuyo precio está dentro del límite.
     */
    fun findByPrice(criteria: PriceFilter): List<ProductView> {
        val sql = """
            SELECT p.id, p.nombre, c.nombre AS categoria, p.precio, p.stock
            FROM productos p
            JOIN categorias c ON c.id = p.categoria_id
            WHERE p.precio <= ?
            ORDER BY p.precio
        """.trimIndent()

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setBigDecimal(1, criteria.maxPrice)

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
