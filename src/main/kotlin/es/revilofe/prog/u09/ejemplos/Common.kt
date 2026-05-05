package es.revilofe.prog.u09.ejemplos

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.h2.jdbcx.JdbcDataSource
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.Date
import java.time.LocalDate
import javax.sql.DataSource

/**
 * Representa un producto persistido en la base de datos de ejemplo.
 *
 * @property id identificador único del producto.
 * @property nombre nombre comercial del producto.
 * @property categoria nombre de la categoría a la que pertenece.
 * @property precio importe actual del producto.
 * @property stock unidades disponibles.
 */
data class ProductView(
    val id: Long,
    val nombre: String,
    val categoria: String,
    val precio: BigDecimal,
    val stock: Int
)

/**
 * Representa un cliente de la base de datos de ejemplo.
 *
 * @property id identificador del cliente.
 * @property nombre nombre completo del cliente.
 * @property email correo electrónico único.
 */
data class Customer(
    val id: Long,
    val nombre: String,
    val email: String
)

/**
 * Representa un pedido simplificado del dominio de ejemplo.
 *
 * @property id identificador del pedido.
 * @property fecha fecha en la que se registró.
 * @property total importe total persistido.
 */
data class OrderSummary(
    val id: Long,
    val fecha: LocalDate,
    val total: BigDecimal
)

/**
 * Agrupa un cliente y sus pedidos para ejemplos de mapeo uno a muchos.
 *
 * @property cliente cliente recuperado.
 * @property pedidos pedidos asociados al cliente.
 */
data class CustomerWithOrders(
    val cliente: Customer,
    val pedidos: List<OrderSummary>
)

/**
 * Centraliza la creación y recreación de la base de datos H2 usada por todos los ejemplos.
 *
 * Se declara como `object` porque no necesitamos varias instancias de esta clase auxiliar:
 * Kotlin genera un singleton y todos los ejemplos comparten el mismo punto de entrada a la
 * configuasociados con las taskración. En una aplicación real esta responsabilidad suele vivir en la configuración
 * del framework o en un contenedor de dependencias.
 */
object DemoDatabase {
    private val databaseFileBase: Path =
        Path.of("build", "h2", "ud9-demo").toAbsolutePath().normalize()

    /**
     * URL JDBC común para todos los ejemplos.
     *
     * `MODE=PostgreSQL` hace que H2 acepte una sintaxis más parecida a PostgreSQL. Es útil en
     * clase porque permite practicar con una base de datos ligera sin alejarse demasiado de un
     * motor relacional habitual en producción.
     */
    val jdbcUrl: String =
        "jdbc:h2:file:${databaseFileBase};MODE=PostgreSQL;DATABASE_TO_UPPER=false;AUTO_SERVER=TRUE"

    private const val USER = "sa"
    private const val PASSWORD = ""

    /**
     * Recrea la base de datos completa y vuelve a cargar los datos semilla.
     *
     * Cada `main` llama a este método para que el ejemplo sea repetible: el alumnado puede
     * ejecutarlo varias veces sin depender de datos que hayan quedado de una ejecución anterior.
     */
    fun reset() {
        Files.createDirectories(databaseFileBase.parent)
        deleteIfExists("$databaseFileBase.mv.db")
        deleteIfExists("$databaseFileBase.trace.db")

        newConnection().use { connection ->
            createSchema(connection)
            insertSeedData(connection)
        }
    }

    /**
     * Crea una conexión JDBC directa con `DriverManager`.
     *
     * @return conexión nueva lista para usar.
     */
    fun newConnection(): Connection = java.sql.DriverManager.getConnection(jdbcUrl, USER, PASSWORD)

    /**
     * Crea un `DataSource` simple sin pool.
     *
     * `DataSource` es una abstracción estándar de JDBC. Permite que repositorios y servicios
     * dependan de un contrato general en lugar de conocer `DriverManager` directamente, lo que
     * encaja con inversión de dependencias y facilita sustituir la forma de obtener conexiones.
     *
     * @return origen de datos basado en H2.
     */
    fun dataSource(): DataSource =
        JdbcDataSource().apply {
            setURL(jdbcUrl)
            user = USER
            password = PASSWORD
        }

    /**
     * Crea un `DataSource` con HikariCP para ejemplos de pool.
     *
     * Un pool mantiene conexiones preparadas para reutilizarlas. En aplicaciones reales evita
     * abrir una conexión física nueva para cada consulta, que es una operación costosa.
     *
     * @param poolName nombre visible del pool.
     * @param maxPoolSize número máximo de conexiones simultáneas.
     * @return pool configurado para la base de datos de ejemplo.
     */
    fun hikariDataSource(
        poolName: String = "U9Pool",
        maxPoolSize: Int = 4
    ): HikariDataSource {
        val config = HikariConfig().apply {
            jdbcUrl = DemoDatabase.jdbcUrl
            username = USER
            password = PASSWORD
            maximumPoolSize = maxPoolSize
            this.poolName = poolName
        }

        return HikariDataSource(config)
    }

    private fun deleteIfExists(path: String) {
        Files.deleteIfExists(Path.of(path))
    }

    /**
     * Crea el esquema relacional mínimo que comparten los ejemplos.
     *
     * Las claves primarias, claves foráneas y restricciones `CHECK` están aquí para mostrar que
     * parte de la consistencia también pertenece a la base de datos, no solo al código Kotlin.
     */
    private fun createSchema(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.execute(
                """
                CREATE TABLE categorias (
                    id BIGINT PRIMARY KEY,
                    nombre VARCHAR(80) NOT NULL UNIQUE
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE productos (
                    id BIGINT PRIMARY KEY,
                    nombre VARCHAR(120) NOT NULL,
                    precio DECIMAL(10, 2) NOT NULL CHECK (precio >= 0),
                    stock INT NOT NULL CHECK (stock >= 0),
                    categoria_id BIGINT NOT NULL,
                    CONSTRAINT fk_producto_categoria
                        FOREIGN KEY (categoria_id) REFERENCES categorias(id)
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE clientes (
                    id BIGINT PRIMARY KEY,
                    nombre VARCHAR(120) NOT NULL,
                    email VARCHAR(120) NOT NULL UNIQUE
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE pedidos (
                    id BIGINT PRIMARY KEY,
                    cliente_id BIGINT NOT NULL,
                    fecha DATE NOT NULL,
                    total DECIMAL(10, 2) NOT NULL CHECK (total >= 0),
                    CONSTRAINT fk_pedido_cliente
                        FOREIGN KEY (cliente_id) REFERENCES clientes(id)
                )
                """.trimIndent()
            )
            statement.execute(
                """
                CREATE TABLE lineas_pedido (
                    id BIGINT PRIMARY KEY,
                    pedido_id BIGINT NOT NULL,
                    producto_id BIGINT NOT NULL,
                    unidades INT NOT NULL CHECK (unidades > 0),
                    subtotal DECIMAL(10, 2) NOT NULL CHECK (subtotal >= 0),
                    CONSTRAINT fk_linea_pedido
                        FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
                    CONSTRAINT fk_linea_producto
                        FOREIGN KEY (producto_id) REFERENCES productos(id)
                )
                """.trimIndent()
            )
        }
    }

    /**
     * Inserta datos semilla mediante sentencias preparadas y lotes.
     *
     * Aunque los datos sean fijos, se usa `PreparedStatement` para mantener el mismo patrón que
     * se aplicaría con datos externos. `addBatch` agrupa varias operaciones del mismo tipo y
     * reduce llamadas repetidas a la base de datos.
     */
    private fun insertSeedData(connection: Connection) {
        connection.prepareStatement(
            "INSERT INTO categorias (id, nombre) VALUES (?, ?)"
        ).use { statement ->
            listOf(
                1L to "Periféricos",
                2L to "Monitores",
                3L to "Almacenamiento"
            ).forEach { (id, nombre) ->
                statement.setLong(1, id)
                statement.setString(2, nombre)
                statement.addBatch()
            }
            statement.executeBatch()
        }

        connection.prepareStatement(
            """
            INSERT INTO productos (id, nombre, precio, stock, categoria_id)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            val products = listOf(
                listOf(1L, "Teclado mecánico", BigDecimal("79.90"), 12, 1L),
                listOf(2L, "Ratón vertical", BigDecimal("39.95"), 7, 1L),
                listOf(3L, "Monitor 27 pulgadas", BigDecimal("219.00"), 5, 2L),
                listOf(4L, "SSD 1TB", BigDecimal("99.50"), 9, 3L)
            )
            products.forEach { values ->
                statement.setLong(1, values[0] as Long)
                statement.setString(2, values[1] as String)
                statement.setBigDecimal(3, values[2] as BigDecimal)
                statement.setInt(4, values[3] as Int)
                statement.setLong(5, values[4] as Long)
                statement.addBatch()
            }
            statement.executeBatch()
        }

        connection.prepareStatement(
            "INSERT INTO clientes (id, nombre, email) VALUES (?, ?, ?)"
        ).use { statement ->
            listOf(
                listOf(1L, "Ana Torres", "ana@example.com"),
                listOf(2L, "Luis Vega", "luis@example.com"),
                listOf(3L, "Marta Gil", "marta@example.com")
            ).forEach { values ->
                statement.setLong(1, values[0] as Long)
                statement.setString(2, values[1] as String)
                statement.setString(3, values[2] as String)
                statement.addBatch()
            }
            statement.executeBatch()
        }

        connection.prepareStatement(
            "INSERT INTO pedidos (id, cliente_id, fecha, total) VALUES (?, ?, ?, ?)"
        ).use { statement ->
            listOf(
                listOf(1L, 1L, LocalDate.of(2026, 4, 20), BigDecimal("119.85")),
                listOf(2L, 1L, LocalDate.of(2026, 4, 21), BigDecimal("219.00")),
                listOf(3L, 2L, LocalDate.of(2026, 4, 22), BigDecimal("99.50"))
            ).forEach { values ->
                statement.setLong(1, values[0] as Long)
                statement.setLong(2, values[1] as Long)
                statement.setDate(3, Date.valueOf(values[2] as LocalDate))
                statement.setBigDecimal(4, values[3] as BigDecimal)
                statement.addBatch()
            }
            statement.executeBatch()
        }

        connection.prepareStatement(
            """
            INSERT INTO lineas_pedido (id, pedido_id, producto_id, unidades, subtotal)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            listOf(
                listOf(1L, 1L, 1L, 1, BigDecimal("79.90")),
                listOf(2L, 1L, 2L, 1, BigDecimal("39.95")),
                listOf(3L, 2L, 3L, 1, BigDecimal("219.00")),
                listOf(4L, 3L, 4L, 1, BigDecimal("99.50"))
            ).forEach { values ->
                statement.setLong(1, values[0] as Long)
                statement.setLong(2, values[1] as Long)
                statement.setLong(3, values[2] as Long)
                statement.setInt(4, values[3] as Int)
                statement.setBigDecimal(5, values[4] as BigDecimal)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }
}

/**
 * Imprime un encabezado legible para separar la salida de cada ejemplo.
 *
 * @param title título del ejemplo en ejecución.
 */
fun printExampleTitle(title: String) {
    println()
    println("=".repeat(72))
    println(title)
    println("=".repeat(72))
}
