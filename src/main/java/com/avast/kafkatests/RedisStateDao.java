package com.avast.kafkatests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Confirmations of messages with storage in Redis.
 */
public class RedisStateDao implements StateDao {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisStateDao.class);

    private static final String KEY_PREFIX_SEND = "send:";
    private static final String KEY_PREFIX_CONFIRM = "confirm:";
    private static final String KEY_PREFIX_CONSUME_AUTO_COMMIT = "consume_auto_commit:";
    private static final String KEY_PREFIX_CHECKS = "checks:";
    private static final String KEY_PREFIX_CONSUME_SEEKING = "consume_seeking:";

    private static final String KEY_MESSAGES_SEND = "messages_send";
    private static final String KEY_MESSAGES_SEND_CONFIRM = "messages_send_confirm";
    private static final String KEY_MESSAGES_SEND_FAIL = "messages_send_fail";
    private static final String KEY_MESSAGES_CONSUME_AUTO_COMMIT = "messages_consume_auto_commit";
    private static final String KEY_MESSAGES_CONSUME_SEEKING = "messages_consume_seeking";
    private static final String KEY_MESSAGES_CONSUME_SEEKING_SKIP = "messages_consume_seeking_skip";

    private static final String KEY_DUPLICATIONS_SEND = "duplications_send";
    private static final String KEY_DUPLICATIONS_SEND_CONFIRM = "duplications_send_confirm";
    private static final String KEY_DUPLICATIONS_CONSUME_AUTO_COMMIT = "duplications_consume_auto_commit";
    private static final String KEY_DUPLICATIONS_CONSUME_SEEKING = "duplications_consume_seeking";

    private static final String KEY_BITS_SUCCESS = "bits_success";
    private static final String KEY_BITS_FAILURE_SEND = "bits_failure_send";
    private static final String KEY_BITS_FAILURE_CONFIRM = "bits_failure_confirm";
    private static final String KEY_BITS_FAILURE_CONSUME_AUTO_COMMIT = "bits_failure_consume_auto_commit";
    private static final String KEY_BITS_FAILURE_CONSUME_SEEKING = "bits_failure_consume_seeking";

    private final JedisPool redisPool;

    public RedisStateDao(String redisServer) {
        redisPool = new JedisPool(redisServer);
    }

    @Override
    public void close() {
        LOGGER.info("Closing instance");
        redisPool.close();
    }

    @Override
    public void markSend(UUID key, int value) {
        setBit(KEY_PREFIX_SEND + key, value, KEY_MESSAGES_SEND, KEY_DUPLICATIONS_SEND);
    }

    @Override
    public void markSendConfirm(UUID key, int value) {
        setBit(KEY_PREFIX_CONFIRM + key, value, KEY_MESSAGES_SEND_CONFIRM, KEY_DUPLICATIONS_SEND_CONFIRM);
    }

    @Override
    public void markSendFail(UUID key, int value) {
        try (Jedis redis = redisPool.getResource()) {
            redis.incr(KEY_MESSAGES_SEND_FAIL);
        }
    }

    @Override
    public void markConsumeAutoCommit(UUID key, int value) {
        setBit(KEY_PREFIX_CONSUME_AUTO_COMMIT + key, value, KEY_MESSAGES_CONSUME_AUTO_COMMIT, KEY_DUPLICATIONS_CONSUME_AUTO_COMMIT);
    }

    private void setBit(String key, int position, String counter, String duplicationsCounter) {
        try (Jedis redis = redisPool.getResource()) {
            boolean duplication = redis.setbit(key, position, true);

            if (duplication) {
                LOGGER.warn("Duplication: {}, {}, {}", duplicationsCounter, key, position);
                redis.incr(duplicationsCounter);
            }

            redis.incr(counter);
        }
    }

    @Override
    public void markConsumeSeeking(UUID key, int value) {
        setBit(KEY_PREFIX_CONSUME_SEEKING + key, value, KEY_MESSAGES_CONSUME_SEEKING, KEY_DUPLICATIONS_CONSUME_SEEKING);
    }

    @Override
    public void markConsumeSeekingSkip(UUID key, int value) {
        try (Jedis redis = redisPool.getResource()) {
            redis.incr(KEY_MESSAGES_CONSUME_SEEKING_SKIP);
        }
    }

    @Override
    public List<GroupState> listGroupStates() {
        try (Jedis redis = redisPool.getResource()) {
            Set<String> keys = readAllKeysWithPrefix(redis, KEY_PREFIX_SEND);
            keys.addAll(readAllKeysWithPrefix(redis, KEY_PREFIX_CONFIRM));
            keys.addAll(readAllKeysWithPrefix(redis, KEY_PREFIX_CONSUME_AUTO_COMMIT));
            keys.addAll(readAllKeysWithPrefix(redis, KEY_PREFIX_CONSUME_SEEKING));

            return keys.stream()
                    .map(k -> new GroupState(UUID.fromString(k),
                            redis.bitcount(KEY_PREFIX_SEND + k).intValue(),
                            redis.bitcount(KEY_PREFIX_CONFIRM + k).intValue(),
                            redis.bitcount(KEY_PREFIX_CONSUME_AUTO_COMMIT + k).intValue(),
                            redis.bitcount(KEY_PREFIX_CONSUME_SEEKING + k).intValue(),
                            readInt(redis, KEY_PREFIX_CHECKS + k)))
                    .collect(Collectors.toList());
        }
    }

    private Set<String> readAllKeysWithPrefix(Jedis redis, String prefix) {
        return redis.keys(prefix + "*")
                .stream()
                .map(k -> k.replace(prefix, ""))
                .collect(Collectors.toSet());
    }

    private int readInt(Jedis redis, String key) {
        String value = redis.get(key);

        if (value == null) {
            return 0;
        }

        return Integer.parseInt(value);
    }

    @Override
    public void success(UUID key, int messagesPerGroup) {
        try (Jedis redis = redisPool.getResource()) {
            redis.incrBy(KEY_BITS_SUCCESS, messagesPerGroup);
            cleanup(redis, key);
        }
    }

    @Override
    public void failure(UUID key, int send, int confirm, int consume, int consumeSeeking) {
        try (Jedis redis = redisPool.getResource()) {
            redis.incrBy(KEY_BITS_FAILURE_SEND, send);
            redis.incrBy(KEY_BITS_FAILURE_CONFIRM, confirm);
            redis.incrBy(KEY_BITS_FAILURE_CONSUME_AUTO_COMMIT, consume);
            redis.incrBy(KEY_BITS_FAILURE_CONSUME_SEEKING, consume);
            cleanup(redis, key);
        }
    }

    private void cleanup(Jedis redis, UUID key) {
        redis.del(KEY_PREFIX_SEND + key,
                KEY_PREFIX_CONFIRM + key,
                KEY_PREFIX_CONSUME_AUTO_COMMIT + key,
                KEY_PREFIX_CONSUME_SEEKING + key,
                KEY_PREFIX_CHECKS + key);
    }

    @Override
    public void markChecks(UUID key) {
        try (Jedis redis = redisPool.getResource()) {
            redis.incr(KEY_PREFIX_CHECKS + key);
        }
    }

    @Override
    public TotalState totalState() {
        try (Jedis redis = redisPool.getResource()) {
            return new TotalState(
                    readInt(redis, KEY_MESSAGES_SEND),
                    readInt(redis, KEY_MESSAGES_SEND_CONFIRM),
                    readInt(redis, KEY_MESSAGES_SEND_FAIL),
                    readInt(redis, KEY_MESSAGES_CONSUME_AUTO_COMMIT),
                    readInt(redis, KEY_MESSAGES_CONSUME_SEEKING),
                    readInt(redis, KEY_MESSAGES_CONSUME_SEEKING_SKIP),

                    readInt(redis, KEY_DUPLICATIONS_SEND),
                    readInt(redis, KEY_DUPLICATIONS_SEND_CONFIRM),
                    readInt(redis, KEY_DUPLICATIONS_CONSUME_AUTO_COMMIT),
                    readInt(redis, KEY_DUPLICATIONS_CONSUME_SEEKING),

                    readInt(redis, KEY_BITS_SUCCESS),
                    readInt(redis, KEY_BITS_FAILURE_SEND),
                    readInt(redis, KEY_BITS_FAILURE_CONFIRM),
                    readInt(redis, KEY_BITS_FAILURE_CONSUME_AUTO_COMMIT),
                    readInt(redis, KEY_BITS_FAILURE_CONSUME_SEEKING)
            );
        }
    }
}
