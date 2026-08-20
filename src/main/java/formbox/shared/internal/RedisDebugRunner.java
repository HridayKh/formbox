package formbox.shared.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Component
public class RedisDebugRunner implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(RedisDebugRunner.class);

	private final RedisConnectionFactory connectionFactory;
	private final StringRedisTemplate redisTemplate;

	public RedisDebugRunner(RedisConnectionFactory connectionFactory, StringRedisTemplate redisTemplate) {
		this.connectionFactory = connectionFactory;
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void run(String... args) {
		log.info("========== [REDIS DEBUG START] ==========");

		// 1. Connection Ping & Latency Check
		try (RedisConnection connection = connectionFactory.getConnection()) {
			Instant start = Instant.now();
			String pingResult = connection.ping();
			long latencyMs = Duration.between(start, Instant.now()).toMillis();

			log.info("Redis Ping Status : SUCCESS");
			log.info("Redis Ping Response: {}", pingResult);
			log.info("Redis Ping Latency : {} ms", latencyMs);
			log.info("Connection Factory : {}", connectionFactory.getClass().getName());
		} catch (Exception e) {
			log.error("CRITICAL: Failed to connect or PING Redis instance!", e);
			log.info("========== [REDIS DEBUG END] ==========\n");
			return;
		}

		// 2. Read / Write / Delete Test
		String testKey = "formbox:debug:" + UUID.randomUUID();
		String testValue = "connection-ok-" + System.currentTimeMillis();

		try {
			log.info("Testing Read/Write operations...");

			// WRITE
			redisTemplate.opsForValue().set(testKey, testValue, Duration.ofSeconds(10));
			log.info("WRITE key [{}] -> OK", testKey);

			// READ
			String retrievedValue = redisTemplate.opsForValue().get(testKey);
			log.info("READ key [{}] -> Retrieved: {}", testKey, retrievedValue);

			if (testValue.equals(retrievedValue)) {
				log.info("READ/WRITE Integrity Check : PASSED");
			} else {
				log.error("READ/WRITE Integrity Check : FAILED (Mismatched values)");
			}

			// DELETE
			Boolean deleted = redisTemplate.delete(testKey);
			log.info("DELETE key [{}] -> Success: {}", testKey, deleted);

		} catch (Exception e) {
			log.error("ERROR: Failed during Redis Read/Write execution!", e);
		}

		log.info("========== [REDIS DEBUG END] ==========\n");
	}
}