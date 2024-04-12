package uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "booker_prisoner_visitor")
class BookerPrisonerVisitor(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  val id: Long = 0,

  @Column(name = "booker_prisoner_id", nullable = false)
  val bookerPrisonerId: Long,

  @ManyToOne
  @JoinColumn(name = "booker_prisoner_id", updatable = false, insertable = false)
  val bookerPrisoner: BookerPrisoner,

  @Column(name = "visitor_id", nullable = false)
  val visitorId: Long,

  @Column(nullable = false)
  val active: Boolean,
) {
  @CreationTimestamp
  @Column
  val createTimestamp: LocalDateTime? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as BookerPrisonerVisitor

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String {
    return this::class.simpleName + "(id=$id, created = $createTimestamp)"
  }
}
