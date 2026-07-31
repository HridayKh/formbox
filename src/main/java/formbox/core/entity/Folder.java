package formbox.core.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "folders")
@Data
public class Folder {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "tenant_id", nullable = false)
	private UUID tenantId;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "created_at")
	@ColumnDefault("NOW()")
	private OffsetDateTime createdAt = OffsetDateTime.now();

}