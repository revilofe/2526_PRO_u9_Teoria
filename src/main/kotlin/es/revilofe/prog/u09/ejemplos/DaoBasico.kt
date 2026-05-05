package es.revilofe.prog.u09.ejemplos

import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `DaoBasico`.
 *
 * Muestra el uso mínimo de un DAO: el `main` crea una implementación JDBC y consulta clientes a
 * través de la interfaz.
 *
 * Es un `object` porque solo contiene un `main` de demostración y no necesita instancias.
 */
object DaoBasicoSimple {
    /**
     * Punto de entrada del ejemplo.
     *
     * `@JvmStatic` genera una función `main` estática para que Gradle pueda lanzarla como clase
     * Java convencional.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("DaoBasico_simple")

        val dao: BasicCustomerDao = JdbcBasicCustomerDao(DemoDatabase.dataSource())
        println(dao.findAll())
    }
}

/**
 * Ejecuta la versión completa del ejemplo `DaoBasico`.
 *
 * Aporta un servicio de consulta que depende del contrato DAO, separando el caso de uso de la
 * implementación JDBC concreta.
 *
 * El singleton compone DAO y servicio para mostrar la separación de capas.
 */
object DaoBasicoCompleto {
    /**
     * Punto de entrada estático de la versión completa.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("DaoBasico_completo")

        val dao: BasicCustomerDao = JdbcBasicCustomerDao(DemoDatabase.dataSource())
        val service = BasicCustomerQueryService(dao)
        println(service.listCustomers())
    }
}

/**
 * Define el contrato mínimo de acceso a datos para clientes.
 *
 * El resto del código depende de esta interfaz y no de la implementación JDBC. Es una versión
 * pequeña del principio de inversión de dependencias: los casos de uso conocen contratos, no
 * detalles técnicos.
 */
private interface BasicCustomerDao {
    /**
     * Recupera todos los clientes disponibles.
     *
     * @return lista de clientes ordenada por identificador.
     */
    fun findAll(): List<Customer>
}

/**
 * Implementación JDBC del DAO de clientes.
 *
 * El patrón DAO concentra el SQL en una clase específica. Así el servicio no mezcla reglas de
 * aplicación con detalles como `PreparedStatement`, columnas o cierre de recursos.
 *
 * @property dataSource origen de datos JDBC.
 */
private class JdbcBasicCustomerDao(
    private val dataSource: DataSource
) : BasicCustomerDao {
    override fun findAll(): List<Customer> {
        val customers = mutableListOf<Customer>()

        dataSource.connection.use { connection ->
            connection.prepareStatement("SELECT id, nombre, email FROM clientes ORDER BY id").use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        customers += Customer(
                            id = resultSet.getLong("id"),
                            nombre = resultSet.getString("nombre"),
                            email = resultSet.getString("email")
                        )
                    }
                }
            }
        }

        return customers
    }
}

/**
 * Servicio que consulta clientes sin depender de JDBC directamente.
 *
 * La clase existe aunque el caso sea simple para mostrar separación de responsabilidades: el DAO
 * sabe hablar con la base de datos y el servicio representa el caso de uso.
 *
 * @property customerDao contrato DAO usado por la capa de aplicación.
 */
private class BasicCustomerQueryService(
    private val customerDao: BasicCustomerDao
) {
    /**
     * Devuelve todos los clientes disponibles.
     *
     * @return lista de clientes leída desde el DAO.
     */
    fun listCustomers(): List<Customer> = customerDao.findAll()
}
