package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events

import jakarta.validation.constraints.NotBlank
import tools.jackson.databind.annotation.JsonDeserialize
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events.deserializers.RawJsonDeserializer

data class DomainEvent(
  @field:NotBlank
  val eventType: String,

  @param:JsonDeserialize(using = RawJsonDeserializer::class)
  @field:NotBlank
  val additionalInformation: String,

  val personReference: PersonReference,
)

data class PersonReference(
  val identifiers: List<PersonIdentifier>,
)

data class PersonIdentifier(val type: Identifier, val value: String)

@Suppress("unused")
enum class Identifier {
  NOMS,
  DPS_CONTACT_ID,
}
