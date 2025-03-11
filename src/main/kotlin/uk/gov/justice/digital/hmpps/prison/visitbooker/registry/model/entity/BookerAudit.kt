package uk.gov.justice.digital.hmpps.prison.visitbooker.registry.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "booker_audit")
class BookerAudit(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  val id: Long = 0,

  @Column(name = "booker_reference", nullable = false)
  val bookerReference: String,

  @Column(name = "text", nullable = false)
  val text: String,
) {
  @CreationTimestamp
  @Column
  val createTimestamp: LocalDateTime? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as PermittedVisitor

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String = this::class.simpleName + "(id=$id, created = $createTimestamp)"
}
