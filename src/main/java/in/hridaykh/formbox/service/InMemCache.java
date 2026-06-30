package in.hridaykh.formbox.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemCache implements ICacheService {

	private final Map<String, String> storage = new ConcurrentHashMap<>();

	@Override
	public void set(String key, String value, long ttlSeconds) {
		storage.put(key, value);
	}

	@Override
	public void set(String key, String value) {
		set(key, value, 3600);
	}

	@Override
	public Optional<String> get(String key) {
		return Optional.ofNullable(storage.get(key));
	}

	@Override
	public void delete(String key) {
		storage.remove(key);
	}
}