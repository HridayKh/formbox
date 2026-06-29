package in.hridaykh.formbox.service;

import java.util.Optional;

public interface ICacheService {
	void set(String key, String value);

	Optional<String> get(String key);

	void delete(String key);
}
