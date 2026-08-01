package formbox.folder;

import java.io.Serializable;
import java.util.UUID;

public record FolderDto(
	UUID id,
	UUID tenantId,
	String name
) implements Serializable {
}