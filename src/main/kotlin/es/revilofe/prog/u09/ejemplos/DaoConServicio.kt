package es.revilofe.prog.u09.ejemplos

import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `DaoConServicio`.
 *
 * Muestra un servicio de aplicación que recibe directamente un DAO JDBC para obtener correos de
 * clientes.
 *
 * Se usa `object` como singleton lanzable porque no hay estado que instanciar.
 */
object DaoConServicioSimple {
    /**
     * Punto de entrada del ejemplo.
     *
     * `@JvmStatic` expone el método como `main` estático para el lanzador JVM.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("DaoConServicio_simple")

        val service = CustomerApplicationService(
            customerDao = JdbcCustomerCatalogDao(DemoDatabase.dataSource())
        )
        println(service.findCustomerEmails())
    }
}

/**
 * Ejecuta la versión completa del ejemplo `DaoConServicio`.
 *
 * Aporta una factoría de DAOs para desacoplar la creación de implementaciones JDBC y dejar al
 * servicio dependiente solo de contratos.
 *
 * El singleton contiene la composición de factoría, DAO y servicio.
 */
object DaoConServicioCompleto {
    /**
     * Punto de entrada estático de la versión completa.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("DaoConServicio_completo")

        val factory = JdbcDaoFactory(DemoDatabase.dataSource())
        val service = CustomerApplicationService(factory.createCustomerDao())
        println(service.findCustomerEmails())
    }
}

/**
 * Contrato de factoría de DAO para desacoplar la construcción.
 *
 * La factoría muestra un patrón de creación: quien necesita un DAO no tiene por qué conocer la
 * clase concreta ni cómo se le pasa el `DataSource`.
 */
private interface CustomerDaoFactory {
    /**
     * Crea un DAO de clientes.
     *
     * @return implementación concreta del DAO.
     */
    fun createCustomerDao(): CustomerCatalogDao
}

/**
 * Contrato de consulta del catálogo de clientes.
 */
private interface CustomerCatalogDao {
    /**
     * Recupera todos los clientes disponibles.
     *
     * @return catálogo completo de clientes.
     */
    fun findAll(): List<Customer>
}

/**
 * Factoría JDBC de DAOs.
 *
 * @property dataSource origen de datos compartido.
 */
private class JdbcDaoFactory(
    private val dataSource: DataSource
) : CustomerDaoFactory {
    override fun createCustomerDao(): CustomerCatalogDao = JdbcCustomerCatalogDao(dataSource)
}

/**
 * DAO JDBC concreto para clientes.
 *
 * @property dataSource origen de datos de la aplicación.
 */
private class JdbcCustomerCatalogDao(
    private val dataSource: DataSource
) : CustomerCatalogDao {
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
 * Servicio de aplicación que depende del contrato DAO y no de JDBC.
 *
 * Esta dependencia por interfaz permite cambiar la persistencia sin tocar el caso de uso, una
 * aplicación directa de inversión de dependencias y del principio abierto/cerrado.
 *
 * @property customerDao acceso a datos inyectado por constructor.
 */
private class CustomerApplicationService(
    private val customerDao: CustomerCatalogDao
) {
    /**
     * Devuelve la lista de correos de cliente para una vista simple.
     *
     * @return correos extraídos del catálogo.
     */
    fun findCustomerEmails(): List<String> = customerDao.findAll().map(Customer::email)
}
