package es.revilofe.prog.u09.ejemplos

import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `MapeoFilaAObjeto`.
 *
 * Muestra el paso manual de una fila de `ResultSet` a un objeto `ProductView` después de un
 * `JOIN` entre productos y categorías.
 *
 * El `object` funciona como singleton lanzador; no representa un producto ni otro concepto del
 * dominio.
 */
object MapeoFilaAObjetoSimple {
    /**
     * Punto de entrada del ejemplo.
     *
     * `@JvmStatic` fuerza la firma estática necesaria para ejecutarlo desde tareas Java.
     */
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
                        // El `ResultSet` es una estructura tabular y mutable. Convertir la fila a
                        // un objeto Kotlin evita que el resto del programa dependa de nombres de
                        // columnas o de la API JDBC.
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
 *
 * Aporta un `ProductRowMapper` reutilizable y un repositorio que delega el mapeo, reduciendo la
 * duplicación cuando varias consultas devuelven la misma forma de datos.
 *
 * Se usa `object` para contener el `main` y la composición del ejemplo.
 */
object MapeoFilaAObjetoCompleto {
    /**
     * Punto de entrada estático de la versión completa.
     */
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
 *
 * Separar el mapeo en una clase pequeña aplica responsabilidad única: el repositorio decide qué
 * consultar y el mapper decide cómo traducir una fila SQL a un objeto del programa.
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
