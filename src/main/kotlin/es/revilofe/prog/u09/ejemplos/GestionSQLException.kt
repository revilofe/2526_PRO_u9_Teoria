package es.revilofe.prog.u09.ejemplos

import java.sql.SQLException
import javax.sql.DataSource

/**
 * Ejecuta la versión simple del ejemplo `GestionSQLException`.
 *
 * Provoca una restricción de email duplicado para observar una `SQLException`, su `SQLState` y el
 * mensaje técnico que devuelve el driver.
 *
 * El `object` es un singleton lanzable para agrupar el `main` del ejemplo simple.
 */
object GestionSQLExceptionSimple {
    /**
     * Punto de entrada del ejemplo.
     *
     * `@JvmStatic` evita que el `main` quede solo como método de la instancia singleton Kotlin.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("GestionSQLException_simple")

        try {
            DemoDatabase.newConnection().use { connection ->
                connection.prepareStatement(
                    "INSERT INTO clientes (id, nombre, email) VALUES (?, ?, ?)"
                ).use { statement ->
                    statement.setLong(1, 10L)
                    statement.setString(2, "Cliente repetido")
                    statement.setString(3, "ana@example.com")
                    statement.executeUpdate()
                }
            }
        } catch (exception: SQLException) {
            println("SQLState: ${exception.sqlState}")
            println("Mensaje: ${exception.message}")
        }
    }
}

/**
 * Ejecuta la versión completa del ejemplo `GestionSQLException`.
 *
 * Aporta una capa de servicio que valida datos y traduce la excepción SQL a una excepción de
 * dominio, conservando la causa original para diagnóstico.
 *
 * El singleton permite ejecutar la demo sin crear una clase auxiliar instanciable.
 */
object GestionSQLExceptionCompleto {
    /**
     * Punto de entrada estático de la versión completa.
     */
    @JvmStatic
    fun main(args: Array<String>) {
        DemoDatabase.reset()
        printExampleTitle("GestionSQLException_completo")

        val service = CustomerRegistrationService(CustomerRepository(DemoDatabase.dataSource()))

        try {
            service.register(Customer(10L, "Cliente repetido", "ana@example.com"))
        } catch (exception: DomainPersistenceException) {
            println("Error de dominio: ${exception.message}")
        }
    }
}

/**
 * Señala un error de persistencia traducido a un lenguaje más cercano al dominio.
 *
 * Mantiene la excepción original como `cause` para no perder información técnica durante el
 * diagnóstico, pero expone un mensaje que entiende la capa de aplicación.
 */
private class DomainPersistenceException(message: String, cause: Throwable) : RuntimeException(message, cause)

/**
 * Servicio de alta de clientes con traducción de errores SQL.
 *
 * @property repository repositorio de clientes.
 */
private class CustomerRegistrationService(
    private val repository: CustomerRepository
) {
    /**
     * Intenta registrar un cliente y traduce errores técnicos a errores comprensibles.
     *
     * @param customer cliente a registrar.
     */
    fun register(customer: Customer) {
        require(customer.email.contains("@")) { "El email debe tener un formato mínimo válido." }

        try {
            repository.insert(customer)
        } catch (exception: SQLException) {
            // La capa de servicio traduce un detalle técnico de JDBC a un mensaje del dominio.
            // Así el resto de la aplicación no queda acoplado a códigos SQLState.
            throw DomainPersistenceException(
                message = if (exception.sqlState == "23505") {
                    "Ya existe un cliente con ese correo electrónico."
                } else {
                    "No se ha podido registrar el cliente."
                },
                cause = exception
            )
        }
    }
}

/**
 * Repositorio JDBC mínimo para altas de cliente.
 *
 * @property dataSource origen de datos de la aplicación.
 */
private class CustomerRepository(
    private val dataSource: DataSource
) {
    /**
     * Inserta un cliente nuevo.
     *
     * @param customer cliente a persistir.
     * @throws SQLException si la base de datos rechaza la operación.
     */
    @Throws(SQLException::class)
    fun insert(customer: Customer) {
        val sql = "INSERT INTO clientes (id, nombre, email) VALUES (?, ?, ?)"

        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setLong(1, customer.id)
                statement.setString(2, customer.nombre)
                statement.setString(3, customer.email)
                statement.executeUpdate()
            }
        }
    }
}
