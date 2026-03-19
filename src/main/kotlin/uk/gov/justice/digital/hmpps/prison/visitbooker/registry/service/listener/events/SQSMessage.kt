package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.service.listener.events

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class SQSMessage(
  @field:NotBlank
  @param:JsonProperty("Type")
  val type: String,
  @field:NotBlank
  @param:JsonProperty("Message")
  val message: String,
  @param:JsonProperty("MessageId")
  val messageId: String? = null,
)
