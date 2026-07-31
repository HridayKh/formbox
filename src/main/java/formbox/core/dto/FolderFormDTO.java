package formbox.core.dto;

import formbox.core.entity.Folder;

import java.util.List;

public record FolderFormDTO(
	Folder folder,
	List<CachedForm> forms
) {
}
