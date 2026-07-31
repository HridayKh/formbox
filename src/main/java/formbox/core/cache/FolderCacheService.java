package formbox.core.cache;

import formbox.shared.constant.CacheNames;
import formbox.core.entity.Folder;
import formbox.shared.Tenant;
import formbox.core.repository.FolderRepository;
import formbox.shared.TenantRepository;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FolderCacheService {

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final TenantRepository tenantRepository;
	private final FolderRepository folderRepository;

	@Cacheable(value = CacheNames.TENANT_FOLDERS, key = "#tenantId.toString()")
	@WithSpan
	public List<Folder> getTenantFolders(UUID tenantId) {
		String cacheKey = String.format("formbox:%s:%s", CacheNames.TENANT_FOLDERS, tenantId);

		String cachedJson = redisTemplate.opsForValue().get(cacheKey);
		if (cachedJson != null) {
			try {
				log.trace("Redis L2 cache HIT for tenant folders list on tenant ID: {}", tenantId);
				return objectMapper.readValue(cachedJson, new TypeReference<>() {
				});
			} catch (Exception e) {
				log.error("Failed to parse tenant folders collection payload from Redis context for tenant: {}", tenantId, e);
			}
		}

		log.debug("Redis L2 cache MISS for tenant folders on tenant ID: {}. Loading relations from database...", tenantId);
		Tenant tenant = tenantRepository.getReferenceById(tenantId);
		List<Folder> dbFolders = folderRepository.findAllByTenant(tenant);
		log.trace("Database query completed. Found {} active folders for tenant ID: {}", dbFolders.size(), tenantId);

		try {
			redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(dbFolders), Duration.ofDays(2));
			log.trace("Tenant folders cache array backfilled successfully for tenant ID: {}", tenantId);
		} catch (Exception e) {
			log.error("Failed to populate tenant folders payload buffer inside Redis for tenant: {}", tenantId, e);
		}

		return dbFolders;
	}

	@CacheEvict(value = CacheNames.TENANT_FOLDERS, key = "#tenantId.toString()")
	@WithSpan
	public void evictTenantFolders(UUID tenantId) {
		log.debug("Request received to drop tenant folders collection cache for tenant ID: {}", tenantId);
		try {
			Boolean deleted = redisTemplate.delete(String.format("formbox:%s:%s", CacheNames.TENANT_FOLDERS, tenantId));
			log.trace("Tenant folders clear task evaluated for tenant ID {}: {}", tenantId, deleted);
		} catch (Exception e) {
			log.error("Failed to purge tenant folders cache collection tracker from Redis cluster for tenant: {}", tenantId, e);
		}
	}
}