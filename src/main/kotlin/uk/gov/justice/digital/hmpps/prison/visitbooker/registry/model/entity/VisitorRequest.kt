package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.dto.enums.VisitorRequestsStatus
import uk.gov.justice.digital.hmpps.prison.visitbooker.registry.utils.QuotableEncoder
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "visitor_requests")
class VisitorRequest(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  val id: Long = 0,

  @Column(nullable = false)
  val bookerReference: String,

  @Column(nullable = false)
  val prisonerId: String,

  @Column(nullable = false)
  val firstName: String,

  @Column(nullable = false)
  val lastName: String,

  @Column(nullable = false)
  val dateOfBirth: LocalDate,

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val status: VisitorRequestsStatus,
) {
  @Column
  var reference: String = ""
    private set

  @PostPersist
  fun createReference() {
    if (reference.isBlank()) {
      reference = QuotableEncoder(minLength = 10).encode(id)
    }
  }

  @CreationTimestamp
  @Column
  val createTimestamp: LocalDateTime? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Booker

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String = this::class.simpleName + "(id=$id, reference='$reference', created = $createTimestamp)"
}
