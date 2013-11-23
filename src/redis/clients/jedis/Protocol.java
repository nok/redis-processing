package redis.clients.jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.util.RedisInputStream;
import redis.clients.util.RedisOutputStream;
import redis.clients.util.SafeEncoder;

public final class Protocol {

    public static final int DEFAULT_PORT = 6379;
    public static final int DEFAULT_SENTINEL_PORT = 26379;
    public static final int DEFAULT_TIMEOUT = 2000;
    public static final int DEFAULT_DATABASE = 0;

    public static final String CHARSET = "UTF-8";

    public static final byte DOLLAR_BYTE = '$';
    public static final byte ASTERISK_BYTE = '*';
    public static final byte PLUS_BYTE = '+';
    public static final byte MINUS_BYTE = '-';
    public static final byte COLON_BYTE = ':';

    public static final String SENTINEL_MASTERS = "masters";
    public static final String SENTINEL_GET_MASTER_ADDR_BY_NAME = "get-master-addr-by-name";
    public static final String SENTINEL_RESET = "reset";
    public static final String SENTINEL_SLAVES = "slaves";
    public static final String SENTINEL_IS_MASTER_DOWN_BY_ADDR = "is-master-down-by-addr";

    private Protocol() {
	// this prevent the class from instantiation
    }

    public static void sendCommand(final RedisOutputStream os,
	    final Command command, final byte[]... args) {
	sendCommand(os, command.raw, args);
    }

    private static void sendCommand(final RedisOutputStream os,
	    final byte[] command, final byte[]... args) {
	try {
	    os.write(ASTERISK_BYTE);
	    os.writeIntCrLf(args.length + 1);
	    os.write(DOLLAR_BYTE);
	    os.writeIntCrLf(command.length);
	    os.write(command);
	    os.writeCrLf();

	    for (final byte[] arg : args) {
		os.write(DOLLAR_BYTE);
		os.writeIntCrLf(arg.length);
		os.write(arg);
		os.writeCrLf();
	    }
	} catch (IOException e) {
	    throw new JedisConnectionException(e);
	}
    }

    private static void processError(final RedisInputStream is) {
	String message = is.readLine();
	throw new JedisDataException(message);
    }

    private static Object process(final RedisInputStream is) {
	try {
	    byte b = is.readByte();
	    if (b == MINUS_BYTE) {
		processError(is);
	    } else if (b == ASTERISK_BYTE) {
		return processMultiBulkReply(is);
	    } else if (b == COLON_BYTE) {
		return processInteger(is);
	    } else if (b == DOLLAR_BYTE) {
		return processBulkReply(is);
	    } else if (b == PLUS_BYTE) {
		return processStatusCodeReply(is);
	    } else {
		throw new JedisConnectionException("Unknown reply: " + (char) b);
	    }
	} catch (IOException e) {
	    throw new JedisConnectionException(e);
	}
	return null;
    }

    private static byte[] processStatusCodeReply(final RedisInputStream is) {
	return SafeEncoder.encode(is.readLine());
    }

    private static byte[] processBulkReply(final RedisInputStream is) {
	int len = Integer.parseInt(is.readLine());
	if (len == -1) {
	    return null;
	}
	byte[] read = new byte[len];
	int offset = 0;
	try {
	    while (offset < len) {
		offset += is.read(read, offset, (len - offset));
	    }
	    // read 2 more bytes for the command delimiter
	    is.readByte();
	    is.readByte();
	} catch (IOException e) {
	    throw new JedisConnectionException(e);
	}

	return read;
    }

    private static Long processInteger(final RedisInputStream is) {
	String num = is.readLine();
	return Long.valueOf(num);
    }

    private static List<Object> processMultiBulkReply(final RedisInputStream is) {
	int num = Integer.parseInt(is.readLine());
	if (num == -1) {
	    return null;
	}
	List<Object> ret = new ArrayList<Object>(num);
	for (int i = 0; i < num; i++) {
	    try {
		ret.add(process(is));
	    } catch (JedisDataException e) {
		ret.add(e);
	    }
	}
	return ret;
    }

    public static Object read(final RedisInputStream is) {
	return process(is);
    }

    public static final byte[] toByteArray(final boolean value) {
    return toByteArray(value ? 1 : 0);
    }

    public static final byte[] toByteArray(final int value) {
	return SafeEncoder.encode(String.valueOf(value));
    }

    public static final byte[] toByteArray(final long value) {
	return SafeEncoder.encode(String.valueOf(value));
    }

    public static final byte[] toByteArray(final double value) {
	return SafeEncoder.encode(String.valueOf(value));
    }

    public static enum Command {
	PING, SET, GET, QUIT, EXISTS, DEL, TYPE, FLUSHDB, KEYS, RANDOMKEY, RENAME, RENAMENX, RENAMEX, DBSIZE, EXPIRE, EXPIREAT, TTL, SELECT, MOVE, FLUSHALL, GETSET, MGET, SETNX, SETEX, MSET, MSETNX, DECRBY, DECR, INCRBY, INCR, APPEND, SUBSTR, HSET, HGET, HSETNX, HMSET, HMGET, HINCRBY, HEXISTS, HDEL, HLEN, HKEYS, HVALS, HGETALL, RPUSH, LPUSH, LLEN, LRANGE, LTRIM, LINDEX, LSET, LREM, LPOP, RPOP, RPOPLPUSH, SADD, SMEMBERS, SREM, SPOP, SMOVE, SCARD, SISMEMBER, SINTER, SINTERSTORE, SUNION, SUNIONSTORE, SDIFF, SDIFFSTORE, SRANDMEMBER, ZADD, ZRANGE, ZREM, ZINCRBY, ZRANK, ZREVRANK, ZREVRANGE, ZCARD, ZSCORE, MULTI, DISCARD, EXEC, WATCH, UNWATCH, SORT, BLPOP, BRPOP, AUTH, SUBSCRIBE, PUBLISH, UNSUBSCRIBE, PSUBSCRIBE, PUNSUBSCRIBE, ZCOUNT, ZRANGEBYSCORE, ZREVRANGEBYSCORE, ZREMRANGEBYRANK, ZREMRANGEBYSCORE, ZUNIONSTORE, ZINTERSTORE, SAVE, BGSAVE, BGREWRITEAOF, LASTSAVE, SHUTDOWN, INFO, MONITOR, SLAVEOF, CONFIG, STRLEN, SYNC, LPUSHX, PERSIST, RPUSHX, ECHO, LINSERT, DEBUG, BRPOPLPUSH, SETBIT, GETBIT, SETRANGE, GETRANGE, EVAL, EVALSHA, SCRIPT, SLOWLOG, OBJECT, BITCOUNT, BITOP, SENTINEL, 
	DUMP, RESTORE, PEXPIRE, PEXPIREAT, PTTL, INCRBYFLOAT, PSETEX, CLIENT, TIME, MIGRATE, HINCRBYFLOAT;

	public final byte[] raw;

	Command() {
	    raw = SafeEncoder.encode(this.name());
	}
    }

    public static enum Keyword {
	AGGREGATE, ALPHA, ASC, BY, DESC, GET, LIMIT, MESSAGE, NO, NOSORT, PMESSAGE, PSUBSCRIBE, PUNSUBSCRIBE, OK, ONE, QUEUED, SET, STORE, SUBSCRIBE, UNSUBSCRIBE, WEIGHTS, WITHSCORES, RESETSTAT, RESET, FLUSH, EXISTS, LOAD, KILL, LEN, REFCOUNT, ENCODING, IDLETIME, AND, OR, XOR, NOT,
	GETNAME, SETNAME,LIST;
	public final byte[] raw;

	Keyword() {
	    raw = SafeEncoder.encode(this.name().toLowerCase());
	}
    }
}
