package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.config

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.BookerAlreadyExistsException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.BookerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.BookerPrisonerAlreadyExistsException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.BookerPrisonerVisitorAlreadyExistsException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.PrisonerNotFoundException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.PrisonerValidationException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.RegisterPrisonerValidationException
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.exception.VisitorForPrisonerBookerNotFoundException
import java.util.*

@RestControllerAdvice
class HmppsPrisonVisitBookerRegistryExceptionHandler {

  private companion object {
    private val LOG = LoggerFactory.getLogger(this::class.java)
  }

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException::class)
  fun handleAccessDeniedException(e: org.springframework.security.access.AccessDeniedException): ResponseEntity<ErrorResponse?>? {
    LOG.debug("Access denied exception caught: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.FORBIDDEN)
      .body(
        ErrorResponse(
          status = HttpStatus.FORBIDDEN,
          userMessage = "Access is forbidden",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(BookerNotFoundException::class)
  fun handleBookerNotFoundException(e: BookerNotFoundException): ResponseEntity<ErrorResponse?>? {
    LOG.debug("Booker not found exception caught: {}", e.message)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND,
          userMessage = "Booker not found",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(BookerAlreadyExistsException::class)
  fun handleBookerAlreadyExistsException(e: BookerAlreadyExistsException): ResponseEntity<ErrorResponse?>? {
    LOG.debug("Booker already exists exception caught: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Booker already exists",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(BookerPrisonerAlreadyExistsException::class)
  fun handleBookerPrisonerAlreadyExistsException(e: BookerPrisonerAlreadyExistsException): ResponseEntity<ErrorResponse?>? {
    LOG.debug("Booker prisoner already exists exception caught: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Booker prisoner already exists",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(BookerPrisonerVisitorAlreadyExistsException::class)
  fun handleBookerPrisonerVisitorAlreadyExistsException(e: BookerPrisonerVisitorAlreadyExistsException): ResponseEntity<ErrorResponse?>? {
    LOG.debug("Booker prisoner visitor already exists exception caught: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Booker prisoner visitor already exists",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(DataIntegrityViolationException::class)
  fun handleDataIntegrityViolationException(e: DataIntegrityViolationException): ResponseEntity<ErrorResponse?>? {
    LOG.debug("DataBase exception caught: {}", e.message)

    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = NOT_FOUND,
          userMessage = "DataBase exception",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(PrisonerNotFoundException::class)
  fun handleBookerPrisonerNotFoundException(e: PrisonerNotFoundException): ResponseEntity<ErrorResponse?>? {
    LOG.debug("Permitted prisoner not found for booker exception caught: {}", e.message)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND,
          userMessage = "Permitted prisoner not found for booker",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(VisitorForPrisonerBookerNotFoundException::class)
  fun handleBookerPrisonerNotFoundException(e: VisitorForPrisonerBookerNotFoundException): ResponseEntity<ErrorResponse?>? {
    LOG.debug("Visitor not found for prisoner booker exception caught: {}", e.message)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND,
          userMessage = "Visitor not found",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(BAD_REQUEST)
    .body(
      ErrorResponse(
        status = BAD_REQUEST,
        userMessage = "Validation failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { LOG.info("Validation exception: {}", e.message) }

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(NOT_FOUND)
    .body(
      ErrorResponse(
        status = NOT_FOUND,
        userMessage = "No resource found failure: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { LOG.info("No resource found exception: {}", e.message) }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleMethodArgumentNotValidException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse?>? {
    LOG.info("Bad Request invalid argument {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Invalid Argument: ${e.fieldError?.field ?: run { e.message}}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(HandlerMethodValidationException::class)
  fun handleMethodValidationException(e: HandlerMethodValidationException): ResponseEntity<ErrorResponse?>? {
    LOG.info("${e.statusCode} invalid argument {}", e.message)
    return ResponseEntity
      .status(e.statusCode)
      .body(
        ErrorResponse(
          status = e.statusCode.value(),
          userMessage = "Invalid Argument in request",
          developerMessage = getJSRMessage(e),
        ),
      )
  }

  private fun getJSRMessage(e: HandlerMethodValidationException): String {
    val errorMessage = StringJoiner(", ")
    e.parameterValidationResults.forEach { parameterValidationResult ->
      parameterValidationResult.resolvableErrors.forEach {
        errorMessage.add(it.codes?.first() ?: it.defaultMessage)
      }
    }
    return errorMessage.toString()
  }

  @ExceptionHandler(Exception::class)
  fun handleException(e: Exception): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(INTERNAL_SERVER_ERROR)
    .body(
      ErrorResponse(
        status = INTERNAL_SERVER_ERROR,
        userMessage = "Unexpected error: ${e.message}",
        developerMessage = e.message,
      ),
    ).also { LOG.error("Unexpected exception", e) }

  @ExceptionHandler(PrisonerValidationException::class)
  fun handlePrisonerValidationException(e: PrisonerValidationException): ResponseEntity<BookerPrisonerValidationErrorResponse?>? {
    LOG.error("Validation exception", e)
    return ResponseEntity
      .status(HttpStatus.UNPROCESSABLE_ENTITY)
      .body(
        BookerPrisonerValidationErrorResponse(
          validationError = e.error.name,
        ),
      )
  }

  @ExceptionHandler(RegisterPrisonerValidationException::class)
  fun handleRegisterPrisonerValidationException(e: RegisterPrisonerValidationException): ResponseEntity<ErrorResponse?>? {
    LOG.error("Validation exception", e)
    return ResponseEntity
      .status(HttpStatus.UNPROCESSABLE_ENTITY)
      .body(
        ErrorResponse(
          status = HttpStatus.UNPROCESSABLE_ENTITY,
          errorCode = null,
          userMessage = "Prisoner registration validation failed",
          developerMessage = "Prisoner registration validation failed with the following errors - ${e.errors.joinToString()}",
          moreInfo = null,
        ),
      )
  }
}

open class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
  val moreInfo: String? = null,
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null,
  ) :
    this(status.value(), errorCode, userMessage, developerMessage, moreInfo)
}

data class BookerPrisonerValidationErrorResponse(
  val validationError: String,
) : ErrorResponse(status = HttpStatus.UNPROCESSABLE_ENTITY)
