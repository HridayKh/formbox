package in.hridaykh.formbox.model;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.jetbrains.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "todo", schema = "public")
public class Todo {

	@Id
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "desc")
	@Nullable
	private String desc;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "meta", columnDefinition = "jsonb")
	private Map<String, Object> meta;

	@Override
	public String toString() {
		return String.format("Todo[id={},createdAt={},name={},desc={},meta={}", id, createdAt, name, desc == null ? "NULL" : desc, meta);
	}
}