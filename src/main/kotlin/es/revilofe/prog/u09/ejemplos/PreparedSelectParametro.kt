package es.revilofe.prog.u09.ejemplos

import java.math.BigDecimal
import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `PreparedSelectParametro`.
 *
 * Practica una consulta filtrada por precio usando un marcador `?` y asignando el parámetro con
 * `setBigDecimal`.
 *
 * Es un `object` porque representa un lanzador singleton, no un modelo con identidad propia.
 */
object PreparedSelectParametroSimple {
    /**
     * Punto de entrada del ejemplo.
     *
     * `@JvmStatic` hace visible este método como `main` estático para la JVM.
     */
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
                // Los `?` son marcadores de posición. El driver envía el valor separado del SQL,
                // evitando concatenaciones y reduciendo el riesgo de inyección SQL.
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
 *
 * Aporta un objeto de criterio, un repositorio de búsqueda y una comparación explícita entre SQL
 * concatenado inseguro y `PreparedStatement` parametrizado.
 *
 * El singleton contiene solo la composición de objetos necesaria para ejecutar el ejemplo.
 */
object PreparedSelectParametroCompleto {
    /**
     * Punto de entrada estático de la versión completa.
     */
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
                // JDBC numera los parámetros desde 1, no desde 0. Es una convención heredada de
                // la API Java y conviene recordarla al leer `setXxx`.
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
