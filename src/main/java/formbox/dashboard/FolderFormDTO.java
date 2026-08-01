package formbox.dashboard;

import formbox.folder.FolderDto;
import formbox.form.FormDto;

import java.util.List;

public record FolderFormDTO(
	FolderDto folder,
	List<FormDto> forms
) {
}
