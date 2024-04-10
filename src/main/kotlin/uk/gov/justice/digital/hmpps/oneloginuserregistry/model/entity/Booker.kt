package uk.gov.justice.digital.hmpps.oneloginuserregistry.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.PostPersist
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import uk.gov.justice.digital.hmpps.oneloginuserregistry.util.QuotableEncoder
import java.time.LocalDateTime

@Entity
@Table(name = "booker")
class Booker(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID")
  val id: Long = 0,

  @Column(name = "one_login_sub", nullable = true)
  var oneLoginSub: String? = null,

  @Column(name = "email", nullable = false)
  val email: String,

) {
  @Column
  var reference: String = ""
    private set

  @CreationTimestamp
  @Column
  val createTimestamp: LocalDateTime? = null

  @PostPersist
  fun createReference() {
    if (reference.isBlank()) {
      reference = QuotableEncoder(minLength = 10).encode(id)
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as Booker

    return id == other.id
  }

  override fun hashCode(): Int = id.hashCode()

  override fun toString(): String {
    return this::class.simpleName + "(id=$id, reference='$reference', created = $createTimestamp)"
  }
}
