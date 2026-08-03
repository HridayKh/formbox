package formbox.notifs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadService {

	private final String UPLOAD_DIR = "/home/hridaykh/Code/hriday_tech/formbox/files";

	public String uploadFile(InputStream is) throws IOException {
		Path uploadPath = Paths.get(UPLOAD_DIR);
		if (!Files.exists(uploadPath)) {
			Files.createDirectories(uploadPath);
		}

		String filename = UUID.randomUUID() + ".tmp";
		Path targetLocation = uploadPath.resolve(filename);

		log.info("Saving incoming upload to {}", targetLocation);
		Files.copy(is, targetLocation, StandardCopyOption.REPLACE_EXISTING);

		return targetLocation.toString();
	}
}