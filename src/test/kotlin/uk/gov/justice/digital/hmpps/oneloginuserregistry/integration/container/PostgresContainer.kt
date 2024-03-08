package uk.gov.justice.digital.hmpps.oneloginuserregistry.integration.container

import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.IOException
import java.net.ServerSocket

object PostgresContainer {
  private val log = LoggerFactory.getLogger(this::class.java)
  val instance: PostgreSQLContainer<Nothing>? by lazy { startPostgresqlIfNotRunning() }
  private fun startPostgresqlIfNotRunning(): PostgreSQLContainer<Nothing>? {
    if (isPostgresRunning()) {
      return null
    }

    val logConsumer = Slf4jLogConsumer(log).withPrefix("postgresql")

    return PostgreSQLContainer<Nothing>("postgres:15.5").apply {
      withEnv("HOSTNAME_EXTERNAL", "localhost")
      withExposedPorts(5432)
      withDatabaseName("one_login_user_registry")
      withUsername("one_login_user_registry")
      withPassword("one_login_user_registry")
      setWaitStrategy(Wait.forListeningPort())
      withReuse(true)
      start()
      followOutput(logConsumer)
    }
  }

  private fun isPostgresRunning(): Boolean =
    try {
      val serverSocket = ServerSocket(5432)
      serverSocket.localPort == 0
    } catch (e: IOException) {
      true
    }
}
