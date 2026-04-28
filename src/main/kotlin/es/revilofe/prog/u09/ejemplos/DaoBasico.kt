package es.revilofe.prog.u09.ejemplos

import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `DaoBasico`.
 */
object DaoBasicoSimple {
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
 */
object DaoBasicoCompleto {
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
