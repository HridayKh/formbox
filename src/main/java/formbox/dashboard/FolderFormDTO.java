package formbox.dashboard;

import formbox.folder.FolderDto;
import formbox.form.FormDto;
import formbox.form.TenantForm;

import java.util.List;

public record FolderFormDTO(
	FolderDto folder,
	List<TenantForm> forms
) {
}
