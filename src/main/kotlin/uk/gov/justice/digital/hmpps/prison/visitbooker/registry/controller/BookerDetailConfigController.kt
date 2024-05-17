package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.config.ErrorResponse
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreateBookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.ErrorResponseDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.BookerDetailsService

const val PUBLIC_BOOKER_CONFIG_CONTROLLER_PATH: String = "/public/booker/config"
const val CLEAR_BOOKER_CONFIG_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_CONFIG_CONTROLLER_PATH/{bookerReference}"

@RestController
class BookerDetailConfigController(
  val bookerDetailsService: BookerDetailsService,
) {

  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VISIT_BOOKER_CONFIG')")
  @PutMapping(PUBLIC_BOOKER_CONFIG_CONTROLLER_PATH)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Create or Update bookers details",
    description = "Create or Update bookers details",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Have created or updated correctly",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Incorrect request to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseDto::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions for this action",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseDto::class))],
      ),
    ],
  )
  fun createOrUpdateBookerDetails(
    @RequestBody @Valid createBookerDto: CreateBookerDto,
  ): BookerDto {
    return bookerDetailsService.createOrUpdateBookerDetails(createBookerDto)
  }

  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VISIT_BOOKER_CONFIG')")
  @DeleteMapping(CLEAR_BOOKER_CONFIG_CONTROLLER_PATH)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Clear bookers details",
    description = "Clear bookers details, keeps booker reference and email",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Have cleared the booker details",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseDto::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions for this action",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseDto::class))],
      ),
    ],
  )
  fun clearBookerDetails(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
  ): BookerDto {
    return bookerDetailsService.clearBookerDetails(bookerReference)
  }
}
