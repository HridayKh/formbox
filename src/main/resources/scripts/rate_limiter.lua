-- KEYS[1]: The Redis key for the IP (e.g., formbox:iprl:192.168.1.1)
local key = KEYS[1]

-- ARGV[1]: Max bucket capacity (e.g., 20 tokens)
-- ARGV[2]: Refill rate per second (e.g., 2 tokens)
-- ARGV[3]: Current timestamp in seconds (passed from Java)
-- ARGV[4]: Cost of this request (usually 1)
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

-- Calculate how long the key should live in Redis (time to fully refill)
local ttl = math.ceil(capacity / refill_rate)

-- Fetch existing bucket data
local data = redis.call('HMGET', key, 'tokens', 'last_updated')
local tokens = tonumber(data[1])
local last_updated = tonumber(data[2])

if tokens == nil then
    -- First time this IP is hitting the limit, start with a full bucket
    tokens = capacity
    last_updated = now
else
    -- Calculate elapsed time and how many tokens to add
    local elapsed = math.max(0, now - last_updated)
    tokens = math.min(capacity, tokens + (elapsed * refill_rate))
    last_updated = now
end

-- Check if there are enough tokens
if tokens >= requested then
    tokens = tokens - requested
    redis.call('HMSET', key, 'tokens', tokens, 'last_updated', last_updated)
    redis.call('EXPIRE', key, ttl)
    return 1 -- Allowed
else
    -- Still update last_updated to account for time elapsed, but don't deduct tokens
    redis.call('HMSET', key, 'tokens', tokens, 'last_updated', last_updated)
    redis.call('EXPIRE', key, ttl)
    return 0 -- Denied
end
