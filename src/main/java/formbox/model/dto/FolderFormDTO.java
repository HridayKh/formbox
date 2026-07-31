package formbox.model.dto;

import formbox.model.entity.Folder;

import java.util.List;

public record FolderFormDTO(
	Folder folder,
	List<CachedForm> forms
) {
}
