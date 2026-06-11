package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service

import org.springframework.dao.DataIntegrityViolationException
import java.sql.SQLException

internal const val PERMITTED_PRISONER_BOOKER_PRISONER_UNIQUE_INDEX = "ux_permitted_prisoner_booker_prisoner"

private const val POSTGRES_UNIQUE_VIOLATION_SQL_STATE = "23505"

internal fun DataIntegrityViolationException.isUniqueViolationFor(constraintName: String): Boolean = sqlExceptions().any { sqlException ->
    sqlException.sqlState == POSTGRES_UNIQUE_VIOLATION_SQL_STATE &&
      sqlException.message?.contains(constraintName) == true
  }

private fun Throwable.sqlExceptions(): Sequence<SQLException> = sequence {
  var cause: Throwable? = this@sqlExceptions
  while (cause != null) {
    if (cause is SQLException) {
      yield(cause)

      var nextException = cause.nextException
      while (nextException != null) {
        yield(nextException)
        nextException = nextException.nextException
      }
    }

    cause = cause.cause
  }
}
