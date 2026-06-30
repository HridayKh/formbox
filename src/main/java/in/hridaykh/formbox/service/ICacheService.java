package in.hridaykh.formbox.service;

import java.util.Optional;

public interface ICacheService {
	void set(String key, String value);

	void set(String key, String value, long ttlSeconds);

	Optional<String> get(String key);

	void delete(String key);
}
