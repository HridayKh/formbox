package in.hridaykh.formbox.model.dto;

import in.hridaykh.formbox.model.entity.Folder;

import java.util.List;

public record FolderFormDTO(
	Folder folder,
	List<CachedForm> forms
) {
}
