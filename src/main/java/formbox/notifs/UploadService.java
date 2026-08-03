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

	public String uploadFile(InputStream is, String fileName) throws IOException {
		String UPLOAD_DIR = "/home/hridaykh/Code/hriday_tech/formbox/files";
		Path uploadPath = Paths.get(UPLOAD_DIR + "/" + UUID.randomUUID());
		Files.createDirectories(uploadPath);
		Path targetLocation = uploadPath.resolve(fileName);

		log.info("Saving incoming upload to {}", targetLocation);
		Files.copy(is, targetLocation, StandardCopyOption.REPLACE_EXISTING);

		return targetLocation.toString();
	}
}