package com.avast.kafkatests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisException;

import java.util.Collections;
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
    private static final String KEY_PREFIX_CHECKS = "checks:";
    private static final String KEY_PREFIX_CONSUME = "consume:";

    private static final String KEY_MESSAGES_SEND = "messages_send";
    private static final String KEY_MESSAGES_SEND_CONFIRM = "messages_send_confirm";
    private static final String KEY_MESSAGES_SEND_FAIL = "messages_send_fail";
    private static final String KEY_MESSAGES_CONSUME = "messages_consume";
    private static final String KEY_MESSAGES_CONSUME_SEEKING_SKIP = "messages_consume_seeking_skip";

    private static final String KEY_DUPLICATIONS_SEND = "duplications_send";
    private static final String KEY_DUPLICATIONS_SEND_CONFIRM = "duplications_send_confirm";
    private static final String KEY_DUPLICATIONS_CONSUME = "duplications_consume";

    private static final String KEY_BITS_SUCCESS = "bits_success";
    private static final String KEY_BITS_FAILURE_SEND = "bits_failure_send";
    private static final String KEY_BITS_FAILURE_CONFIRM = "bits_failure_confirm";
    private static final String KEY_BITS_FAILURE_CONSUME = "bits_failure_consume";

    private final JedisPool redisPool;

    public RedisStateDao(String redisServer) {
        this.redisPool = new JedisPool(redisServer);
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
        } catch (JedisException e) {
            LOGGER.error("Redis operation failed: {}", e.toString(), e);
        }
    }

    @Override
    public void markConsume(ConsumerType consumerType, UUID key, int value) {
        setBit(KEY_PREFIX_CONSUME + consumerType + ":" + key,
                value,
                KEY_MESSAGES_CONSUME + ":" + consumerType,
                KEY_DUPLICATIONS_CONSUME + ":" + consumerType);
    }

    private void setBit(String key, int position, String counter, String duplicationsCounter) {
        try (Jedis redis = redisPool.getResource()) {
            boolean duplication = redis.setbit(key, position, true);

            if (duplication) {
                LOGGER.warn("Duplication: {}, {}, {}", duplicationsCounter, key, position);
                redis.incr(duplicationsCounter);
            }

            redis.incr(counter);
        } catch (JedisException e) {
            LOGGER.error("Redis operation failed: {}", e.toString(), e);
        }
    }

    @Override
    public void markConsumeSeekingSkip(UUID key, int value) {
        try (Jedis redis = redisPool.getResource()) {
            redis.incr(KEY_MESSAGES_CONSUME_SEEKING_SKIP);
        } catch (JedisException e) {
            LOGGER.error("Redis operation failed: {}", e.toString(), e);
        }
    }

    @Override
    public List<GroupState> listGroupStates(List<ConsumerType> consumerTypes) {
        try (Jedis redis = redisPool.getResource()) {
            Set<String> keys = readAllKeysWithPrefix(redis, KEY_PREFIX_SEND);
            keys.addAll(readAllKeysWithPrefix(redis, KEY_PREFIX_CONFIRM));
            consumerTypes.forEach(t -> keys.addAll(readAllKeysWithPrefix(redis, KEY_PREFIX_CONSUME + t + ":")));

            return keys.stream()
                    .map(k -> new GroupState(UUID.fromString(k),
                            redis.bitcount(KEY_PREFIX_SEND + k).intValue(),
                            redis.bitcount(KEY_PREFIX_CONFIRM + k).intValue(),
                            getConsumerCounts(consumerTypes, redis, k),
                            readInt(redis, KEY_PREFIX_CHECKS + k)))
                    .collect(Collectors.toList());
        } catch (JedisException e) {
            LOGGER.error("Redis operation failed: {}", e.toString(), e);
            return Collections.emptyList();
        }
    }

    private List<ConsumerCount> getConsumerCounts(List<ConsumerType> consumerTypes, Jedis redis, String k) {
        return consumerTypes.stream()
                .map(t -> new ConsumerCount(t, redis.bitcount(KEY_PREFIX_CONSUME + t + ":" + k).intValue()))
                .collect(Collectors.<ConsumerCount>toList());
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
    public void success(GroupState group, int messagesPerGroup) {
        try (Jedis redis = redisPool.getResource()) {
            redis.incrBy(KEY_BITS_SUCCESS, messagesPerGroup);
            cleanup(redis, group);
        } catch (JedisException e) {
            LOGGER.error("Redis operation failed: {}", e.toString(), e);
        }
    }

    @Override
    public void failure(GroupState group, int messagesPerGroup) {
        try (Jedis redis = redisPool.getResource()) {
            int sendFailures = messagesPerGroup - group.getSend();
            if (sendFailures > 0) {
                redis.incrBy(KEY_BITS_FAILURE_SEND, sendFailures);
            }

            int confirmFailures = messagesPerGroup - group.getConfirm();
            if (confirmFailures > 0) {
                redis.incrBy(KEY_BITS_FAILURE_CONFIRM, confirmFailures);
            }

            group.getConsumerCounts().stream()
                    .forEach(e -> {
                        int consumeFailures = messagesPerGroup - e.getCount();
                        if (consumeFailures > 0) {
                            redis.incrBy(KEY_BITS_FAILURE_CONSUME + ":" + e.getConsumerType(), consumeFailures);
                        }
                    });

            cleanup(redis, group);
        } catch (JedisException e) {
            LOGGER.error("Redis operation failed: {}", e.toString(), e);
        }
    }

    private void cleanup(Jedis redis, GroupState group) {
        UUID key = group.getKey();

        List<String> keys = group.getConsumerCounts().stream()
                .map(t -> KEY_PREFIX_CONSUME + t.getConsumerType() + ":" + key)
                .collect(Collectors.toList());

        keys.add(KEY_PREFIX_SEND + key);
        keys.add(KEY_PREFIX_CONFIRM + key);
        keys.add(KEY_PREFIX_CHECKS + key);

        redis.del(keys.toArray(new String[keys.size()]));
    }

    @Override
    public void markChecks(UUID key) {
        try (Jedis redis = redisPool.getResource()) {
            redis.incr(KEY_PREFIX_CHECKS + key);
        } catch (JedisException e) {
            LOGGER.error("Redis operation failed: {}", e.toString(), e);
        }
    }

    @Override
    public TotalState totalState(List<ConsumerType> consumerTypes) {
        try (Jedis redis = redisPool.getResource()) {
            return new TotalState(
                    readInt(redis, KEY_MESSAGES_SEND),
                    readInt(redis, KEY_MESSAGES_SEND_CONFIRM),
                    readInt(redis, KEY_MESSAGES_SEND_FAIL),
                    consumerTypes.stream()
                            .map(t -> new ConsumerCount(t, readInt(redis, KEY_MESSAGES_CONSUME + ":" + t)))
                            .collect(Collectors.toList()),
                    readInt(redis, KEY_MESSAGES_CONSUME_SEEKING_SKIP),

                    readInt(redis, KEY_DUPLICATIONS_SEND),
                    readInt(redis, KEY_DUPLICATIONS_SEND_CONFIRM),
                    consumerTypes.stream()
                            .map(t -> new ConsumerCount(t, readInt(redis, KEY_DUPLICATIONS_CONSUME + ":" + t)))
                            .collect(Collectors.toList()),

                    readInt(redis, KEY_BITS_SUCCESS),
                    readInt(redis, KEY_BITS_FAILURE_SEND),
                    readInt(redis, KEY_BITS_FAILURE_CONFIRM),
                    consumerTypes.stream()
                            .map(t -> new ConsumerCount(t, readInt(redis, KEY_BITS_FAILURE_CONSUME + ":" + t)))
                            .collect(Collectors.toList())
            );
        } catch (JedisException e) {
            LOGGER.error("Redis operation failed: {}", e.toString(), e);
            return TotalState.INSTANCE;
        }
    }
}
