package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.controller.admin

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.config.ErrorResponse
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.BookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreatePermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.CreatePermittedVisitorDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.ErrorResponseDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedPrisonerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.PermittedVisitorDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.SearchBookerDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.UpdateRegisteredPrisonersPrisonDto
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.BookerDetailsService

const val PUBLIC_BOOKER_CONFIG_CONTROLLER_PATH: String = "/public/booker/config"
const val CREATE_BOOKER_PRISONER_PATH: String = "$PUBLIC_BOOKER_CONFIG_CONTROLLER_PATH/{bookerReference}/prisoner"
const val CREATE_BOOKER_PRISONER_VISITOR_PATH: String = "$PUBLIC_BOOKER_CONFIG_CONTROLLER_PATH/{bookerReference}/prisoner/{prisonerId}/visitor"

const val CLEAR_BOOKER_CONFIG_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_CONFIG_CONTROLLER_PATH/{bookerReference}"
const val ACTIVATE_BOOKER_PRISONER_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_CONFIG_CONTROLLER_PATH/{bookerReference}/prisoner/{prisonerId}/activate"
const val DEACTIVATE_BOOKER_PRISONER_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_CONFIG_CONTROLLER_PATH/{bookerReference}/prisoner/{prisonerId}/deactivate"
const val ACTIVATE_BOOKER_PRISONER_VISITOR_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_CONFIG_CONTROLLER_PATH/{bookerReference}/prisoner/{prisonerId}/visitor/{visitorId}/activate"
const val DEACTIVATE_BOOKER_PRISONER_VISITOR_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_CONFIG_CONTROLLER_PATH/{bookerReference}/prisoner/{prisonerId}/visitor/{visitorId}/deactivate"
const val UPDATE_BOOKER_PRISONER_PRISON_CONTROLLER_PATH: String = "$PUBLIC_BOOKER_CONFIG_CONTROLLER_PATH/{bookerReference}/prisoner/{prisonerId}/prison"

const val GET_BOOKER_USING_REFERENCE: String = "$PUBLIC_BOOKER_CONFIG_CONTROLLER_PATH/{bookerReference}"
const val SEARCH_FOR_BOOKER: String = "$PUBLIC_BOOKER_CONFIG_CONTROLLER_PATH/search"

@RestController
class BookerDetailConfigController(
  val bookerDetailsService: BookerDetailsService,
) {
  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VISIT_BOOKER_CONFIG')")
  @PutMapping(CREATE_BOOKER_PRISONER_PATH)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create booker prisoner details",
    description = "Create booker prisoner details",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Have created booker prisoner correctly",
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
      ApiResponse(
        responseCode = "404",
        description = "The booker does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseDto::class))],
      ),
    ],
  )
  fun createBookerPrisoner(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
    @RequestBody @Valid createPermittedPrisonerDto: CreatePermittedPrisonerDto,
  ): PermittedPrisonerDto = bookerDetailsService.createBookerPrisoner(bookerReference, createPermittedPrisonerDto)

  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VISIT_BOOKER_CONFIG')")
  @PutMapping(CREATE_BOOKER_PRISONER_VISITOR_PATH)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
    summary = "Create booker prisoner visitor details",
    description = "Create booker prisoner visitor details",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Have created booker prisoner visitor correctly",
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
      ApiResponse(
        responseCode = "404",
        description = "The booker or the booker prisoner does not exist",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseDto::class))],
      ),
    ],
  )
  fun createBookerPrisonerVisitor(
    @PathVariable(value = "prisonerId", required = true)
    @NotBlank
    prisonerId: String,
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
    @RequestBody @Valid createPermittedVisitorDto: CreatePermittedVisitorDto,
  ): PermittedVisitorDto = bookerDetailsService.createBookerPrisonerVisitor(bookerReference, prisonerId, createPermittedVisitorDto)

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
      ApiResponse(
        responseCode = "404",
        description = "Booker not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseDto::class))],
      ),
    ],
  )
  fun clearBookerDetails(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
  ): BookerDto = bookerDetailsService.clearBookerDetails(bookerReference)

  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VISIT_BOOKER_CONFIG')")
  @GetMapping(GET_BOOKER_USING_REFERENCE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Get bookers details using reference",
    description = "gets bookers details",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Booker details found and returned",
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
      ApiResponse(
        responseCode = "404",
        description = "Booker not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseDto::class))],
      ),
    ],
  )
  fun getBookerDetailsUsingReference(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
  ): BookerDto = bookerDetailsService.getBookerByReference(bookerReference)

  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VISIT_BOOKER_CONFIG')")
  @PutMapping(ACTIVATE_BOOKER_PRISONER_CONTROLLER_PATH)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "activate booker prisoner",
    description = "activate booker prisoner",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Have activated booker prisoner",
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
      ApiResponse(
        responseCode = "404",
        description = "Prisoner not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseDto::class))],
      ),
    ],
  )
  fun activateBookerPrisoner(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
    @PathVariable(value = "prisonerId", required = true)
    @NotBlank
    prisonerId: String,
  ): PermittedPrisonerDto = bookerDetailsService.activateBookerPrisoner(bookerReference, prisonerId)

  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VISIT_BOOKER_CONFIG')")
  @PutMapping(ACTIVATE_BOOKER_PRISONER_VISITOR_CONTROLLER_PATH)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "activate booker prisoner visitor",
    description = "activate booker prisoner visitor",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Have activated booker prisoner visitor",
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
      ApiResponse(
        responseCode = "404",
        description = "Visitor not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseDto::class))],
      ),
    ],
  )
  fun activateBookerPrisonerVisitor(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
    @PathVariable(value = "prisonerId", required = true)
    @NotBlank
    prisonerId: String,
    @PathVariable(value = "visitorId", required = true)
    @NotNull
    visitorId: Long,
  ): PermittedVisitorDto = bookerDetailsService.activateBookerPrisonerVisitor(bookerReference, prisonerId, visitorId)

  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VISIT_BOOKER_CONFIG')")
  @PutMapping(DEACTIVATE_BOOKER_PRISONER_CONTROLLER_PATH)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "deactivate booker prisoner",
    description = "deactivate booker prisoners",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Have deactivated booker prisoner",
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
      ApiResponse(
        responseCode = "404",
        description = "Prisoner not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseDto::class))],
      ),
    ],
  )
  fun deactivateBookerPrisoner(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
    @PathVariable(value = "prisonerId", required = true)
    @NotBlank
    prisonerId: String,
  ): PermittedPrisonerDto = bookerDetailsService.deactivateBookerPrisoner(bookerReference, prisonerId)

  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VISIT_BOOKER_CONFIG')")
  @PutMapping(DEACTIVATE_BOOKER_PRISONER_VISITOR_CONTROLLER_PATH)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "deactivate booker prisoner visitor",
    description = "deactivate booker prisoner visitor",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Have deactivated booker prisoner visitor",
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
      ApiResponse(
        responseCode = "404",
        description = "visitor not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseDto::class))],
      ),
    ],
  )
  fun deactivateBookerPrisonerVisitor(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
    @PathVariable(value = "prisonerId", required = true)
    @NotBlank
    prisonerId: String,
    @PathVariable(value = "visitorId", required = true)
    @NotNull
    visitorId: Long,
  ): PermittedVisitorDto = bookerDetailsService.deactivateBookerPrisonerVisitor(bookerReference, prisonerId, visitorId)

  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VISIT_BOOKER_CONFIG')")
  @PostMapping(SEARCH_FOR_BOOKER)
  @Operation(
    summary = "Search for a booker using specific criteria",
    description = "Search for a booker using specific criteria, returns list (list of 1 entry if only 1 booker is found)",
    requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
      content = [
        Content(
          mediaType = "application/json",
          schema = Schema(implementation = SearchBookerDto::class),
        ),
      ],
    ),
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Booker found via search",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to search for booker",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  fun searchForBooker(
    @RequestBody
    @Valid
    searchBookerDto: SearchBookerDto,
  ): List<BookerDto> = bookerDetailsService.searchForBooker(searchBookerDto)

  @PreAuthorize("hasRole('ROLE_VISIT_BOOKER_REGISTRY__VISIT_BOOKER_CONFIG')")
  @PutMapping(UPDATE_BOOKER_PRISONER_PRISON_CONTROLLER_PATH)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
    summary = "Update a registered prisoner's prison code",
    description = "Update a registered prisoner's prison code",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Registered prisoner's prison code wasupdated successfully",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Validation failure, incorrect request to update registered prisoner's prison code",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseDto::class))],
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
      ApiResponse(
        responseCode = "404",
        description = "booker / prisoner not found",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponseDto::class))],
      ),
    ],
  )
  fun updateBookerPrisonerPrison(
    @PathVariable(value = "bookerReference", required = true)
    @NotBlank
    bookerReference: String,
    @PathVariable(value = "prisonerId", required = true)
    @NotBlank
    prisonerId: String,
    @RequestBody
    @Valid
    updateRegisteredPrisonersPrisonDto: UpdateRegisteredPrisonersPrisonDto,
  ): PermittedPrisonerDto = bookerDetailsService.updateBookerPrisonerPrison(
    bookerReference = bookerReference,
    prisonerId = prisonerId,
    newPrisonCode = updateRegisteredPrisonersPrisonDto.prisonCode,
  )
}
