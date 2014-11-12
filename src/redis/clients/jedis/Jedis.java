package redis.clients.jedis;

import java.net.URI;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import redis.clients.jedis.BinaryClient.LIST_POSITION;
import redis.clients.jedis.JedisCluster.Reset;
import redis.clients.util.Pool;
import redis.clients.util.SafeEncoder;
import redis.clients.util.Slowlog;

public class Jedis extends BinaryJedis implements JedisCommands,
	MultiKeyCommands, AdvancedJedisCommands, ScriptingCommands,
	BasicCommands, ClusterCommands {

    protected Pool<Jedis> dataSource = null;

    public Jedis() {
	super();
    }

    public Jedis(final String host) {
	super(host);
    }

    public Jedis(final String host, final int port) {
	super(host, port);
    }

    public Jedis(final String host, final int port, final int timeout) {
	super(host, port, timeout);
    }

    public Jedis(JedisShardInfo shardInfo) {
	super(shardInfo);
    }

    public Jedis(URI uri) {
	super(uri);
    }

    public Jedis(final URI uri, final int timeout) {
	super(uri, timeout);
    }

    /**
     * Set the string value as value of the key. The string can't be longer than
     * 1073741824 bytes (1 GB).
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     * @param value
     * @return Status code reply
     */
    public String set(final String key, String value) {
	checkIsInMulti();
	client.set(key, value);
	return client.getStatusCodeReply();
    }

    /**
     * Set the string value as value of the key. The string can't be longer than
     * 1073741824 bytes (1 GB).
     * 
     * @param key
     * @param value
     * @param nxxx
     *            NX|XX, NX -- Only set the key if it does not already exist. XX
     *            -- Only set the key if it already exist.
     * @param expx
     *            EX|PX, expire time units: EX = seconds; PX = milliseconds
     * @param time
     *            expire time in the units of {@param #expx}
     * @return Status code reply
     */
    public String set(final String key, final String value, final String nxxx,
	    final String expx, final long time) {
	checkIsInMulti();
	client.set(key, value, nxxx, expx, time);
	return client.getStatusCodeReply();
    }

    /**
     * Get the value of the specified key. If the key does not exist null is
     * returned. If the value stored at key is not a string an error is returned
     * because GET can only handle string values.
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     * @return Bulk reply
     */
    public String get(final String key) {
	checkIsInMulti();
	client.sendCommand(Protocol.Command.GET, key);
	return client.getBulkReply();
    }

    /**
     * Test if the specified key exists. The command returns "1" if the key
     * exists, otherwise "0" is returned. Note that even keys set with an empty
     * string as value will return "1".
     * 
     * Time complexity: O(1)
     * 
     * @param key
     * @return Boolean reply, true if the key exists, otherwise false
     */
    public Boolean exists(final String key) {
	checkIsInMulti();
	client.exists(key);
	return client.getIntegerReply() == 1;
    }

    /**
     * Remove the specified keys. If a given key does not exist no operation is
     * performed for this key. The command returns the number of keys removed.
     * 
     * Time complexity: O(1)
     * 
     * @param keys
     * @return Integer reply, specifically: an integer greater than 0 if one or
     *         more keys were removed 0 if none of the specified key existed
     */
    public Long del(final String... keys) {
	checkIsInMulti();
	client.del(keys);
	return client.getIntegerReply();
    }

    public Long del(String key) {
	client.del(key);
	return client.getIntegerReply();
    }

    /**
     * Return the type of the value stored at key in form of a string. The type
     * can be one of "none", "string", "list", "set". "none" is returned if the
     * key does not exist.
     * 
     * Time complexity: O(1)
     * 
     * @param key
     * @return Status code reply, specifically: "none" if the key does not exist
     *         "string" if the key contains a String value "list" if the key
     *         contains a List value "set" if the key contains a Set value
     *         "zset" if the key contains a Sorted Set value "hash" if the key
     *         contains a Hash value
     */
    public String type(final String key) {
	checkIsInMulti();
	client.type(key);
	return client.getStatusCodeReply();
    }

    /**
     * Returns all the keys matching the glob-style pattern as space separated
     * strings. For example if you have in the database the keys "foo" and
     * "foobar" the command "KEYS foo*" will return "foo foobar".
     * <p>
     * Note that while the time complexity for this operation is O(n) the
     * constant times are pretty low. For example Redis running on an entry
     * level laptop can scan a 1 million keys database in 40 milliseconds.
     * <b>Still it's better to consider this one of the slow commands that may
     * ruin the DB performance if not used with care.</b>
     * <p>
     * In other words this command is intended only for debugging and special
     * operations like creating a script to change the DB schema. Don't use it
     * in your normal code. Use Redis Sets in order to group together a subset
     * of objects.
     * <p>
     * Glob style patterns examples:
     * <ul>
     * <li>h?llo will match hello hallo hhllo
     * <li>h*llo will match hllo heeeello
     * <li>h[ae]llo will match hello and hallo, but not hillo
     * </ul>
     * <p>
     * Use \ to escape special chars if you want to match them verbatim.
     * <p>
     * Time complexity: O(n) (with n being the number of keys in the DB, and
     * assuming keys and pattern of limited length)
     * 
     * @param pattern
     * @return Multi bulk reply
     */
    public Set<String> keys(final String pattern) {
	checkIsInMulti();
	client.keys(pattern);
	return BuilderFactory.STRING_SET
		.build(client.getBinaryMultiBulkReply());
    }

    /**
     * Return a randomly selected key from the currently selected DB.
     * <p>
     * Time complexity: O(1)
     * 
     * @return Singe line reply, specifically the randomly selected key or an
     *         empty string is the database is empty
     */
    public String randomKey() {
	checkIsInMulti();
	client.randomKey();
	return client.getBulkReply();
    }

    /**
     * Atomically renames the key oldkey to newkey. If the source and
     * destination name are the same an error is returned. If newkey already
     * exists it is overwritten.
     * <p>
     * Time complexity: O(1)
     * 
     * @param oldkey
     * @param newkey
     * @return Status code repy
     */
    public String rename(final String oldkey, final String newkey) {
	checkIsInMulti();
	client.rename(oldkey, newkey);
	return client.getStatusCodeReply();
    }

    /**
     * Rename oldkey into newkey but fails if the destination key newkey already
     * exists.
     * <p>
     * Time complexity: O(1)
     * 
     * @param oldkey
     * @param newkey
     * @return Integer reply, specifically: 1 if the key was renamed 0 if the
     *         target key already exist
     */
    public Long renamenx(final String oldkey, final String newkey) {
	checkIsInMulti();
	client.renamenx(oldkey, newkey);
	return client.getIntegerReply();
    }

    /**
     * Set a timeout on the specified key. After the timeout the key will be
     * automatically deleted by the server. A key with an associated timeout is
     * said to be volatile in Redis terminology.
     * <p>
     * Voltile keys are stored on disk like the other keys, the timeout is
     * persistent too like all the other aspects of the dataset. Saving a
     * dataset containing expires and stopping the server does not stop the flow
     * of time as Redis stores on disk the time when the key will no longer be
     * available as Unix time, and not the remaining seconds.
     * <p>
     * Since Redis 2.1.3 you can update the value of the timeout of a key
     * already having an expire set. It is also possible to undo the expire at
     * all turning the key into a normal key using the {@link #persist(String)
     * PERSIST} command.
     * <p>
     * Time complexity: O(1)
     * 
     * @see <ahref="http://code.google.com/p/redis/wiki/ExpireCommand">ExpireCommand</a>
     * 
     * @param key
     * @param seconds
     * @return Integer reply, specifically: 1: the timeout was set. 0: the
     *         timeout was not set since the key already has an associated
     *         timeout (this may happen only in Redis versions < 2.1.3, Redis >=
     *         2.1.3 will happily update the timeout), or the key does not
     *         exist.
     */
    public Long expire(final String key, final int seconds) {
	checkIsInMulti();
	client.expire(key, seconds);
	return client.getIntegerReply();
    }

    /**
     * EXPIREAT works exctly like {@link #expire(String, int) EXPIRE} but
     * instead to get the number of seconds representing the Time To Live of the
     * key as a second argument (that is a relative way of specifing the TTL),
     * it takes an absolute one in the form of a UNIX timestamp (Number of
     * seconds elapsed since 1 Gen 1970).
     * <p>
     * EXPIREAT was introduced in order to implement the Append Only File
     * persistence mode so that EXPIRE commands are automatically translated
     * into EXPIREAT commands for the append only file. Of course EXPIREAT can
     * also used by programmers that need a way to simply specify that a given
     * key should expire at a given time in the future.
     * <p>
     * Since Redis 2.1.3 you can update the value of the timeout of a key
     * already having an expire set. It is also possible to undo the expire at
     * all turning the key into a normal key using the {@link #persist(String)
     * PERSIST} command.
     * <p>
     * Time complexity: O(1)
     * 
     * @see <ahref="http://code.google.com/p/redis/wiki/ExpireCommand">ExpireCommand</a>
     * 
     * @param key
     * @param unixTime
     * @return Integer reply, specifically: 1: the timeout was set. 0: the
     *         timeout was not set since the key already has an associated
     *         timeout (this may happen only in Redis versions < 2.1.3, Redis >=
     *         2.1.3 will happily update the timeout), or the key does not
     *         exist.
     */
    public Long expireAt(final String key, final long unixTime) {
	checkIsInMulti();
	client.expireAt(key, unixTime);
	return client.getIntegerReply();
    }

    /**
     * The TTL command returns the remaining time to live in seconds of a key
     * that has an {@link #expire(String, int) EXPIRE} set. This introspection
     * capability allows a Redis client to check how many seconds a given key
     * will continue to be part of the dataset.
     * 
     * @param key
     * @return Integer reply, returns the remaining time to live in seconds of a
     *         key that has an EXPIRE. In Redis 2.6 or older, if the Key does
     *         not exists or does not have an associated expire, -1 is returned.
     *         In Redis 2.8 or newer, if the Key does not have an associated
     *         expire, -1 is returned or if the Key does not exists, -2 is
     *         returned.
     */
    public Long ttl(final String key) {
	checkIsInMulti();
	client.ttl(key);
	return client.getIntegerReply();
    }

    /**
     * Move the specified key from the currently selected DB to the specified
     * destination DB. Note that this command returns 1 only if the key was
     * successfully moved, and 0 if the target key was already there or if the
     * source key was not found at all, so it is possible to use MOVE as a
     * locking primitive.
     * 
     * @param key
     * @param dbIndex
     * @return Integer reply, specifically: 1 if the key was moved 0 if the key
     *         was not moved because already present on the target DB or was not
     *         found in the current DB.
     */
    public Long move(final String key, final int dbIndex) {
	checkIsInMulti();
	client.move(key, dbIndex);
	return client.getIntegerReply();
    }

    /**
     * GETSET is an atomic set this value and return the old value command. Set
     * key to the string value and return the old value stored at key. The
     * string can't be longer than 1073741824 bytes (1 GB).
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     * @param value
     * @return Bulk reply
     */
    public String getSet(final String key, final String value) {
	checkIsInMulti();
	client.getSet(key, value);
	return client.getBulkReply();
    }

    /**
     * Get the values of all the specified keys. If one or more keys dont exist
     * or is not of type String, a 'nil' value is returned instead of the value
     * of the specified key, but the operation never fails.
     * <p>
     * Time complexity: O(1) for every key
     * 
     * @param keys
     * @return Multi bulk reply
     */
    public List<String> mget(final String... keys) {
	checkIsInMulti();
	client.mget(keys);
	return client.getMultiBulkReply();
    }

    /**
     * SETNX works exactly like {@link #set(String, String) SET} with the only
     * difference that if the key already exists no operation is performed.
     * SETNX actually means "SET if Not eXists".
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     * @param value
     * @return Integer reply, specifically: 1 if the key was set 0 if the key
     *         was not set
     */
    public Long setnx(final String key, final String value) {
	checkIsInMulti();
	client.setnx(key, value);
	return client.getIntegerReply();
    }

    /**
     * The command is exactly equivalent to the following group of commands:
     * {@link #set(String, String) SET} + {@link #expire(String, int) EXPIRE}.
     * The operation is atomic.
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     * @param seconds
     * @param value
     * @return Status code reply
     */
    public String setex(final String key, final int seconds, final String value) {
	checkIsInMulti();
	client.setex(key, seconds, value);
	return client.getStatusCodeReply();
    }

    /**
     * Set the the respective keys to the respective values. MSET will replace
     * old values with new values, while {@link #msetnx(String...) MSETNX} will
     * not perform any operation at all even if just a single key already
     * exists.
     * <p>
     * Because of this semantic MSETNX can be used in order to set different
     * keys representing different fields of an unique logic object in a way
     * that ensures that either all the fields or none at all are set.
     * <p>
     * Both MSET and MSETNX are atomic operations. This means that for instance
     * if the keys A and B are modified, another client talking to Redis can
     * either see the changes to both A and B at once, or no modification at
     * all.
     * 
     * @see #msetnx(String...)
     * 
     * @param keysvalues
     * @return Status code reply Basically +OK as MSET can't fail
     */
    public String mset(final String... keysvalues) {
	checkIsInMulti();
	client.mset(keysvalues);
	return client.getStatusCodeReply();
    }

    /**
     * Set the the respective keys to the respective values.
     * {@link #mset(String...) MSET} will replace old values with new values,
     * while MSETNX will not perform any operation at all even if just a single
     * key already exists.
     * <p>
     * Because of this semantic MSETNX can be used in order to set different
     * keys representing different fields of an unique logic object in a way
     * that ensures that either all the fields or none at all are set.
     * <p>
     * Both MSET and MSETNX are atomic operations. This means that for instance
     * if the keys A and B are modified, another client talking to Redis can
     * either see the changes to both A and B at once, or no modification at
     * all.
     * 
     * @see #mset(String...)
     * 
     * @param keysvalues
     * @return Integer reply, specifically: 1 if the all the keys were set 0 if
     *         no key was set (at least one key already existed)
     */
    public Long msetnx(final String... keysvalues) {
	checkIsInMulti();
	client.msetnx(keysvalues);
	return client.getIntegerReply();
    }

    /**
     * IDECRBY work just like {@link #decr(String) INCR} but instead to
     * decrement by 1 the decrement is integer.
     * <p>
     * INCR commands are limited to 64 bit signed integers.
     * <p>
     * Note: this is actually a string operation, that is, in Redis there are
     * not "integer" types. Simply the string stored at the key is parsed as a
     * base 10 64 bit signed integer, incremented, and then converted back as a
     * string.
     * <p>
     * Time complexity: O(1)
     * 
     * @see #incr(String)
     * @see #decr(String)
     * @see #incrBy(String, long)
     * 
     * @param key
     * @param integer
     * @return Integer reply, this commands will reply with the new value of key
     *         after the increment.
     */
    public Long decrBy(final String key, final long integer) {
	checkIsInMulti();
	client.decrBy(key, integer);
	return client.getIntegerReply();
    }

    /**
     * Decrement the number stored at key by one. If the key does not exist or
     * contains a value of a wrong type, set the key to the value of "0" before
     * to perform the decrement operation.
     * <p>
     * INCR commands are limited to 64 bit signed integers.
     * <p>
     * Note: this is actually a string operation, that is, in Redis there are
     * not "integer" types. Simply the string stored at the key is parsed as a
     * base 10 64 bit signed integer, incremented, and then converted back as a
     * string.
     * <p>
     * Time complexity: O(1)
     * 
     * @see #incr(String)
     * @see #incrBy(String, long)
     * @see #decrBy(String, long)
     * 
     * @param key
     * @return Integer reply, this commands will reply with the new value of key
     *         after the increment.
     */
    public Long decr(final String key) {
	checkIsInMulti();
	client.decr(key);
	return client.getIntegerReply();
    }

    /**
     * INCRBY work just like {@link #incr(String) INCR} but instead to increment
     * by 1 the increment is integer.
     * <p>
     * INCR commands are limited to 64 bit signed integers.
     * <p>
     * Note: this is actually a string operation, that is, in Redis there are
     * not "integer" types. Simply the string stored at the key is parsed as a
     * base 10 64 bit signed integer, incremented, and then converted back as a
     * string.
     * <p>
     * Time complexity: O(1)
     * 
     * @see #incr(String)
     * @see #decr(String)
     * @see #decrBy(String, long)
     * 
     * @param key
     * @param integer
     * @return Integer reply, this commands will reply with the new value of key
     *         after the increment.
     */
    public Long incrBy(final String key, final long integer) {
	checkIsInMulti();
	client.incrBy(key, integer);
	return client.getIntegerReply();
    }

    /**
     * INCRBYFLOAT
     * <p>
     * INCRBYFLOAT commands are limited to double precision floating point
     * values.
     * <p>
     * Note: this is actually a string operation, that is, in Redis there are
     * not "double" types. Simply the string stored at the key is parsed as a
     * base double precision floating point value, incremented, and then
     * converted back as a string. There is no DECRYBYFLOAT but providing a
     * negative value will work as expected.
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     * @param value
     * @return Double reply, this commands will reply with the new value of key
     *         after the increment.
     */
    public Double incrByFloat(final String key, final double value) {
	checkIsInMulti();
	client.incrByFloat(key, value);
	String dval = client.getBulkReply();
	return (dval != null ? new Double(dval) : null);
    }

    /**
     * Increment the number stored at key by one. If the key does not exist or
     * contains a value of a wrong type, set the key to the value of "0" before
     * to perform the increment operation.
     * <p>
     * INCR commands are limited to 64 bit signed integers.
     * <p>
     * Note: this is actually a string operation, that is, in Redis there are
     * not "integer" types. Simply the string stored at the key is parsed as a
     * base 10 64 bit signed integer, incremented, and then converted back as a
     * string.
     * <p>
     * Time complexity: O(1)
     * 
     * @see #incrBy(String, long)
     * @see #decr(String)
     * @see #decrBy(String, long)
     * 
     * @param key
     * @return Integer reply, this commands will reply with the new value of key
     *         after the increment.
     */
    public Long incr(final String key) {
	checkIsInMulti();
	client.incr(key);
	return client.getIntegerReply();
    }

    /**
     * If the key already exists and is a string, this command appends the
     * provided value at the end of the string. If the key does not exist it is
     * created and set as an empty string, so APPEND will be very similar to SET
     * in this special case.
     * <p>
     * Time complexity: O(1). The amortized time complexity is O(1) assuming the
     * appended value is small and the already present value is of any size,
     * since the dynamic string library used by Redis will double the free space
     * available on every reallocation.
     * 
     * @param key
     * @param value
     * @return Integer reply, specifically the total length of the string after
     *         the append operation.
     */
    public Long append(final String key, final String value) {
	checkIsInMulti();
	client.append(key, value);
	return client.getIntegerReply();
    }

    /**
     * Return a subset of the string from offset start to offset end (both
     * offsets are inclusive). Negative offsets can be used in order to provide
     * an offset starting from the end of the string. So -1 means the last char,
     * -2 the penultimate and so forth.
     * <p>
     * The function handles out of range requests without raising an error, but
     * just limiting the resulting range to the actual length of the string.
     * <p>
     * Time complexity: O(start+n) (with start being the start index and n the
     * total length of the requested range). Note that the lookup part of this
     * command is O(1) so for small strings this is actually an O(1) command.
     * 
     * @param key
     * @param start
     * @param end
     * @return Bulk reply
     */
    public String substr(final String key, final int start, final int end) {
	checkIsInMulti();
	client.substr(key, start, end);
	return client.getBulkReply();
    }

    /**
     * 
     * Set the specified hash field to the specified value.
     * <p>
     * If key does not exist, a new key holding a hash is created.
     * <p>
     * <b>Time complexity:</b> O(1)
     * 
     * @param key
     * @param field
     * @param value
     * @return If the field already exists, and the HSET just produced an update
     *         of the value, 0 is returned, otherwise if a new field is created
     *         1 is returned.
     */
    public Long hset(final String key, final String field, final String value) {
	checkIsInMulti();
	client.hset(key, field, value);
	return client.getIntegerReply();
    }

    /**
     * If key holds a hash, retrieve the value associated to the specified
     * field.
     * <p>
     * If the field is not found or the key does not exist, a special 'nil'
     * value is returned.
     * <p>
     * <b>Time complexity:</b> O(1)
     * 
     * @param key
     * @param field
     * @return Bulk reply
     */
    public String hget(final String key, final String field) {
	checkIsInMulti();
	client.hget(key, field);
	return client.getBulkReply();
    }

    /**
     * 
     * Set the specified hash field to the specified value if the field not
     * exists. <b>Time complexity:</b> O(1)
     * 
     * @param key
     * @param field
     * @param value
     * @return If the field already exists, 0 is returned, otherwise if a new
     *         field is created 1 is returned.
     */
    public Long hsetnx(final String key, final String field, final String value) {
	checkIsInMulti();
	client.hsetnx(key, field, value);
	return client.getIntegerReply();
    }

    /**
     * Set the respective fields to the respective values. HMSET replaces old
     * values with new values.
     * <p>
     * If key does not exist, a new key holding a hash is created.
     * <p>
     * <b>Time complexity:</b> O(N) (with N being the number of fields)
     * 
     * @param key
     * @param hash
     * @return Return OK or Exception if hash is empty
     */
    public String hmset(final String key, final Map<String, String> hash) {
	checkIsInMulti();
	client.hmset(key, hash);
	return client.getStatusCodeReply();
    }

    /**
     * Retrieve the values associated to the specified fields.
     * <p>
     * If some of the specified fields do not exist, nil values are returned.
     * Non existing keys are considered like empty hashes.
     * <p>
     * <b>Time complexity:</b> O(N) (with N being the number of fields)
     * 
     * @param key
     * @param fields
     * @return Multi Bulk Reply specifically a list of all the values associated
     *         with the specified fields, in the same order of the request.
     */
    public List<String> hmget(final String key, final String... fields) {
	checkIsInMulti();
	client.hmget(key, fields);
	return client.getMultiBulkReply();
    }

    /**
     * Increment the number stored at field in the hash at key by value. If key
     * does not exist, a new key holding a hash is created. If field does not
     * exist or holds a string, the value is set to 0 before applying the
     * operation. Since the value argument is signed you can use this command to
     * perform both increments and decrements.
     * <p>
     * The range of values supported by HINCRBY is limited to 64 bit signed
     * integers.
     * <p>
     * <b>Time complexity:</b> O(1)
     * 
     * @param key
     * @param field
     * @param value
     * @return Integer reply The new value at field after the increment
     *         operation.
     */
    public Long hincrBy(final String key, final String field, final long value) {
	checkIsInMulti();
	client.hincrBy(key, field, value);
	return client.getIntegerReply();
    }

    /**
     * Increment the number stored at field in the hash at key by a double
     * precision floating point value. If key does not exist, a new key holding
     * a hash is created. If field does not exist or holds a string, the value
     * is set to 0 before applying the operation. Since the value argument is
     * signed you can use this command to perform both increments and
     * decrements.
     * <p>
     * The range of values supported by HINCRBYFLOAT is limited to double
     * precision floating point values.
     * <p>
     * <b>Time complexity:</b> O(1)
     * 
     * @param key
     * @param field
     * @param value
     * @return Double precision floating point reply The new value at field
     *         after the increment operation.
     */
    public Double hincrByFloat(final String key, final String field,
	    final double value) {
	checkIsInMulti();
	client.hincrByFloat(key, field, value);
	final String dval = client.getBulkReply();
	return (dval != null ? new Double(dval) : null);
    }

    /**
     * Test for existence of a specified field in a hash.
     * 
     * <b>Time complexity:</b> O(1)
     * 
     * @param key
     * @param field
     * @return Return 1 if the hash stored at key contains the specified field.
     *         Return 0 if the key is not found or the field is not present.
     */
    public Boolean hexists(final String key, final String field) {
	checkIsInMulti();
	client.hexists(key, field);
	return client.getIntegerReply() == 1;
    }

    /**
     * Remove the specified field from an hash stored at key.
     * <p>
     * <b>Time complexity:</b> O(1)
     * 
     * @param key
     * @param fields
     * @return If the field was present in the hash it is deleted and 1 is
     *         returned, otherwise 0 is returned and no operation is performed.
     */
    public Long hdel(final String key, final String... fields) {
	checkIsInMulti();
	client.hdel(key, fields);
	return client.getIntegerReply();
    }

    /**
     * Return the number of items in a hash.
     * <p>
     * <b>Time complexity:</b> O(1)
     * 
     * @param key
     * @return The number of entries (fields) contained in the hash stored at
     *         key. If the specified key does not exist, 0 is returned assuming
     *         an empty hash.
     */
    public Long hlen(final String key) {
	checkIsInMulti();
	client.hlen(key);
	return client.getIntegerReply();
    }

    /**
     * Return all the fields in a hash.
     * <p>
     * <b>Time complexity:</b> O(N), where N is the total number of entries
     * 
     * @param key
     * @return All the fields names contained into a hash.
     */
    public Set<String> hkeys(final String key) {
	checkIsInMulti();
	client.hkeys(key);
	return BuilderFactory.STRING_SET
		.build(client.getBinaryMultiBulkReply());
    }

    /**
     * Return all the values in a hash.
     * <p>
     * <b>Time complexity:</b> O(N), where N is the total number of entries
     * 
     * @param key
     * @return All the fields values contained into a hash.
     */
    public List<String> hvals(final String key) {
	checkIsInMulti();
	client.hvals(key);
	final List<String> lresult = client.getMultiBulkReply();
	return lresult;
    }

    /**
     * Return all the fields and associated values in a hash.
     * <p>
     * <b>Time complexity:</b> O(N), where N is the total number of entries
     * 
     * @param key
     * @return All the fields and values contained into a hash.
     */
    public Map<String, String> hgetAll(final String key) {
	checkIsInMulti();
	client.hgetAll(key);
	return BuilderFactory.STRING_MAP
		.build(client.getBinaryMultiBulkReply());
    }

    /**
     * Add the string value to the head (LPUSH) or tail (RPUSH) of the list
     * stored at key. If the key does not exist an empty list is created just
     * before the append operation. If the key exists but is not a List an error
     * is returned.
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     * @param strings
     * @return Integer reply, specifically, the number of elements inside the
     *         list after the push operation.
     */
    public Long rpush(final String key, final String... strings) {
	checkIsInMulti();
	client.rpush(key, strings);
	return client.getIntegerReply();
    }

    /**
     * Add the string value to the head (LPUSH) or tail (RPUSH) of the list
     * stored at key. If the key does not exist an empty list is created just
     * before the append operation. If the key exists but is not a List an error
     * is returned.
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     * @param strings
     * @return Integer reply, specifically, the number of elements inside the
     *         list after the push operation.
     */
    public Long lpush(final String key, final String... strings) {
	checkIsInMulti();
	client.lpush(key, strings);
	return client.getIntegerReply();
    }

    /**
     * Return the length of the list stored at the specified key. If the key
     * does not exist zero is returned (the same behaviour as for empty lists).
     * If the value stored at key is not a list an error is returned.
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     * @return The length of the list.
     */
    public Long llen(final String key) {
	checkIsInMulti();
	client.llen(key);
	return client.getIntegerReply();
    }

    /**
     * Return the specified elements of the list stored at the specified key.
     * Start and end are zero-based indexes. 0 is the first element of the list
     * (the list head), 1 the next element and so on.
     * <p>
     * For example LRANGE foobar 0 2 will return the first three elements of the
     * list.
     * <p>
     * start and end can also be negative numbers indicating offsets from the
     * end of the list. For example -1 is the last element of the list, -2 the
     * penultimate element and so on.
     * <p>
     * <b>Consistency with range functions in various programming languages</b>
     * <p>
     * Note that if you have a list of numbers from 0 to 100, LRANGE 0 10 will
     * return 11 elements, that is, rightmost item is included. This may or may
     * not be consistent with behavior of range-related functions in your
     * programming language of choice (think Ruby's Range.new, Array#slice or
     * Python's range() function).
     * <p>
     * LRANGE behavior is consistent with one of Tcl.
     * <p>
     * <b>Out-of-range indexes</b>
     * <p>
     * Indexes out of range will not produce an error: if start is over the end
     * of the list, or start > end, an empty list is returned. If end is over
     * the end of the list Redis will threat it just like the last element of
     * the list.
     * <p>
     * Time complexity: O(start+n) (with n being the length of the range and
     * start being the start offset)
     * 
     * @param key
     * @param start
     * @param end
     * @return Multi bulk reply, specifically a list of elements in the
     *         specified range.
     */
    public List<String> lrange(final String key, final long start,
	    final long end) {
	checkIsInMulti();
	client.lrange(key, start, end);
	return client.getMultiBulkReply();
    }

    /**
     * Trim an existing list so that it will contain only the specified range of
     * elements specified. Start and end are zero-based indexes. 0 is the first
     * element of the list (the list head), 1 the next element and so on.
     * <p>
     * For example LTRIM foobar 0 2 will modify the list stored at foobar key so
     * that only the first three elements of the list will remain.
     * <p>
     * start and end can also be negative numbers indicating offsets from the
     * end of the list. For example -1 is the last element of the list, -2 the
     * penultimate element and so on.
     * <p>
     * Indexes out of range will not produce an error: if start is over the end
     * of the list, or start > end, an empty list is left as value. If end over
     * the end of the list Redis will threat it just like the last element of
     * the list.
     * <p>
     * Hint: the obvious use of LTRIM is together with LPUSH/RPUSH. For example:
     * <p>
     * {@code lpush("mylist", "someelement"); ltrim("mylist", 0, 99); * }
     * <p>
     * The above two commands will push elements in the list taking care that
     * the list will not grow without limits. This is very useful when using
     * Redis to store logs for example. It is important to note that when used
     * in this way LTRIM is an O(1) operation because in the average case just
     * one element is removed from the tail of the list.
     * <p>
     * Time complexity: O(n) (with n being len of list - len of range)
     * 
     * @param key
     * @param start
     * @param end
     * @return Status code reply
     */
    public String ltrim(final String key, final long start, final long end) {
	checkIsInMulti();
	client.ltrim(key, start, end);
	return client.getStatusCodeReply();
    }

    /**
     * Return the specified element of the list stored at the specified key. 0
     * is the first element, 1 the second and so on. Negative indexes are
     * supported, for example -1 is the last element, -2 the penultimate and so
     * on.
     * <p>
     * If the value stored at key is not of list type an error is returned. If
     * the index is out of range a 'nil' reply is returned.
     * <p>
     * Note that even if the average time complexity is O(n) asking for the
     * first or the last element of the list is O(1).
     * <p>
     * Time complexity: O(n) (with n being the length of the list)
     * 
     * @param key
     * @param index
     * @return Bulk reply, specifically the requested element
     */
    public String lindex(final String key, final long index) {
	checkIsInMulti();
	client.lindex(key, index);
	return client.getBulkReply();
    }

    /**
     * Set a new value as the element at index position of the List at key.
     * <p>
     * Out of range indexes will generate an error.
     * <p>
     * Similarly to other list commands accepting indexes, the index can be
     * negative to access elements starting from the end of the list. So -1 is
     * the last element, -2 is the penultimate, and so forth.
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(N) (with N being the length of the list), setting the first or last
     * elements of the list is O(1).
     * 
     * @see #lindex(String, long)
     * 
     * @param key
     * @param index
     * @param value
     * @return Status code reply
     */
    public String lset(final String key, final long index, final String value) {
	checkIsInMulti();
	client.lset(key, index, value);
	return client.getStatusCodeReply();
    }

    /**
     * Remove the first count occurrences of the value element from the list. If
     * count is zero all the elements are removed. If count is negative elements
     * are removed from tail to head, instead to go from head to tail that is
     * the normal behaviour. So for example LREM with count -2 and hello as
     * value to remove against the list (a,b,c,hello,x,hello,hello) will lave
     * the list (a,b,c,hello,x). The number of removed elements is returned as
     * an integer, see below for more information about the returned value. Note
     * that non existing keys are considered like empty lists by LREM, so LREM
     * against non existing keys will always return 0.
     * <p>
     * Time complexity: O(N) (with N being the length of the list)
     * 
     * @param key
     * @param count
     * @param value
     * @return Integer Reply, specifically: The number of removed elements if
     *         the operation succeeded
     */
    public Long lrem(final String key, final long count, final String value) {
	checkIsInMulti();
	client.lrem(key, count, value);
	return client.getIntegerReply();
    }

    /**
     * Atomically return and remove the first (LPOP) or last (RPOP) element of
     * the list. For example if the list contains the elements "a","b","c" LPOP
     * will return "a" and the list will become "b","c".
     * <p>
     * If the key does not exist or the list is already empty the special value
     * 'nil' is returned.
     * 
     * @see #rpop(String)
     * 
     * @param key
     * @return Bulk reply
     */
    public String lpop(final String key) {
	checkIsInMulti();
	client.lpop(key);
	return client.getBulkReply();
    }

    /**
     * Atomically return and remove the first (LPOP) or last (RPOP) element of
     * the list. For example if the list contains the elements "a","b","c" RPOP
     * will return "c" and the list will become "a","b".
     * <p>
     * If the key does not exist or the list is already empty the special value
     * 'nil' is returned.
     * 
     * @see #lpop(String)
     * 
     * @param key
     * @return Bulk reply
     */
    public String rpop(final String key) {
	checkIsInMulti();
	client.rpop(key);
	return client.getBulkReply();
    }

    /**
     * Atomically return and remove the last (tail) element of the srckey list,
     * and push the element as the first (head) element of the dstkey list. For
     * example if the source list contains the elements "a","b","c" and the
     * destination list contains the elements "foo","bar" after an RPOPLPUSH
     * command the content of the two lists will be "a","b" and "c","foo","bar".
     * <p>
     * If the key does not exist or the list is already empty the special value
     * 'nil' is returned. If the srckey and dstkey are the same the operation is
     * equivalent to removing the last element from the list and pusing it as
     * first element of the list, so it's a "list rotation" command.
     * <p>
     * Time complexity: O(1)
     * 
     * @param srckey
     * @param dstkey
     * @return Bulk reply
     */
    public String rpoplpush(final String srckey, final String dstkey) {
	checkIsInMulti();
	client.rpoplpush(srckey, dstkey);
	return client.getBulkReply();
    }

    /**
     * Add the specified member to the set value stored at key. If member is
     * already a member of the set no operation is performed. If key does not
     * exist a new set with the specified member as sole member is created. If
     * the key exists but does not hold a set value an error is returned.
     * <p>
     * Time complexity O(1)
     * 
     * @param key
     * @param members
     * @return Integer reply, specifically: 1 if the new element was added 0 if
     *         the element was already a member of the set
     */
    public Long sadd(final String key, final String... members) {
	checkIsInMulti();
	client.sadd(key, members);
	return client.getIntegerReply();
    }

    /**
     * Return all the members (elements) of the set value stored at key. This is
     * just syntax glue for {@link #sinter(String...) SINTER}.
     * <p>
     * Time complexity O(N)
     * 
     * @param key
     * @return Multi bulk reply
     */
    public Set<String> smembers(final String key) {
	checkIsInMulti();
	client.smembers(key);
	final List<String> members = client.getMultiBulkReply();
	return new HashSet<String>(members);
    }

    /**
     * Remove the specified member from the set value stored at key. If member
     * was not a member of the set no operation is performed. If key does not
     * hold a set value an error is returned.
     * <p>
     * Time complexity O(1)
     * 
     * @param key
     * @param members
     * @return Integer reply, specifically: 1 if the new element was removed 0
     *         if the new element was not a member of the set
     */
    public Long srem(final String key, final String... members) {
	checkIsInMulti();
	client.srem(key, members);
	return client.getIntegerReply();
    }

    /**
     * Remove a random element from a Set returning it as return value. If the
     * Set is empty or the key does not exist, a nil object is returned.
     * <p>
     * The {@link #srandmember(String)} command does a similar work but the
     * returned element is not removed from the Set.
     * <p>
     * Time complexity O(1)
     * 
     * @param key
     * @return Bulk reply
     */
    public String spop(final String key) {
	checkIsInMulti();
	client.spop(key);
	return client.getBulkReply();
    }

    /**
     * Move the specifided member from the set at srckey to the set at dstkey.
     * This operation is atomic, in every given moment the element will appear
     * to be in the source or destination set for accessing clients.
     * <p>
     * If the source set does not exist or does not contain the specified
     * element no operation is performed and zero is returned, otherwise the
     * element is removed from the source set and added to the destination set.
     * On success one is returned, even if the element was already present in
     * the destination set.
     * <p>
     * An error is raised if the source or destination keys contain a non Set
     * value.
     * <p>
     * Time complexity O(1)
     * 
     * @param srckey
     * @param dstkey
     * @param member
     * @return Integer reply, specifically: 1 if the element was moved 0 if the
     *         element was not found on the first set and no operation was
     *         performed
     */
    public Long smove(final String srckey, final String dstkey,
	    final String member) {
	checkIsInMulti();
	client.smove(srckey, dstkey, member);
	return client.getIntegerReply();
    }

    /**
     * Return the set cardinality (number of elements). If the key does not
     * exist 0 is returned, like for empty sets.
     * 
     * @param key
     * @return Integer reply, specifically: the cardinality (number of elements)
     *         of the set as an integer.
     */
    public Long scard(final String key) {
	checkIsInMulti();
	client.scard(key);
	return client.getIntegerReply();
    }

    /**
     * Return 1 if member is a member of the set stored at key, otherwise 0 is
     * returned.
     * <p>
     * Time complexity O(1)
     * 
     * @param key
     * @param member
     * @return Integer reply, specifically: 1 if the element is a member of the
     *         set 0 if the element is not a member of the set OR if the key
     *         does not exist
     */
    public Boolean sismember(final String key, final String member) {
	checkIsInMulti();
	client.sismember(key, member);
	return client.getIntegerReply() == 1;
    }

    /**
     * Return the members of a set resulting from the intersection of all the
     * sets hold at the specified keys. Like in
     * {@link #lrange(String, long, long) LRANGE} the result is sent to the
     * client as a multi-bulk reply (see the protocol specification for more
     * information). If just a single key is specified, then this command
     * produces the same result as {@link #smembers(String) SMEMBERS}. Actually
     * SMEMBERS is just syntax sugar for SINTER.
     * <p>
     * Non existing keys are considered like empty sets, so if one of the keys
     * is missing an empty set is returned (since the intersection with an empty
     * set always is an empty set).
     * <p>
     * Time complexity O(N*M) worst case where N is the cardinality of the
     * smallest set and M the number of sets
     * 
     * @param keys
     * @return Multi bulk reply, specifically the list of common elements.
     */
    public Set<String> sinter(final String... keys) {
	checkIsInMulti();
	client.sinter(keys);
	final List<String> members = client.getMultiBulkReply();
	return new HashSet<String>(members);
    }

    /**
     * This commnad works exactly like {@link #sinter(String...) SINTER} but
     * instead of being returned the resulting set is sotred as dstkey.
     * <p>
     * Time complexity O(N*M) worst case where N is the cardinality of the
     * smallest set and M the number of sets
     * 
     * @param dstkey
     * @param keys
     * @return Status code reply
     */
    public Long sinterstore(final String dstkey, final String... keys) {
	checkIsInMulti();
	client.sinterstore(dstkey, keys);
	return client.getIntegerReply();
    }

    /**
     * Return the members of a set resulting from the union of all the sets hold
     * at the specified keys. Like in {@link #lrange(String, long, long) LRANGE}
     * the result is sent to the client as a multi-bulk reply (see the protocol
     * specification for more information). If just a single key is specified,
     * then this command produces the same result as {@link #smembers(String)
     * SMEMBERS}.
     * <p>
     * Non existing keys are considered like empty sets.
     * <p>
     * Time complexity O(N) where N is the total number of elements in all the
     * provided sets
     * 
     * @param keys
     * @return Multi bulk reply, specifically the list of common elements.
     */
    public Set<String> sunion(final String... keys) {
	checkIsInMulti();
	client.sunion(keys);
	final List<String> members = client.getMultiBulkReply();
	return new HashSet<String>(members);
    }

    /**
     * This command works exactly like {@link #sunion(String...) SUNION} but
     * instead of being returned the resulting set is stored as dstkey. Any
     * existing value in dstkey will be over-written.
     * <p>
     * Time complexity O(N) where N is the total number of elements in all the
     * provided sets
     * 
     * @param dstkey
     * @param keys
     * @return Status code reply
     */
    public Long sunionstore(final String dstkey, final String... keys) {
	checkIsInMulti();
	client.sunionstore(dstkey, keys);
	return client.getIntegerReply();
    }

    /**
     * Return the difference between the Set stored at key1 and all the Sets
     * key2, ..., keyN
     * <p>
     * <b>Example:</b>
     * 
     * <pre>
     * key1 = [x, a, b, c]
     * key2 = [c]
     * key3 = [a, d]
     * SDIFF key1,key2,key3 => [x, b]
     * </pre>
     * 
     * Non existing keys are considered like empty sets.
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(N) with N being the total number of elements of all the sets
     * 
     * @param keys
     * @return Return the members of a set resulting from the difference between
     *         the first set provided and all the successive sets.
     */
    public Set<String> sdiff(final String... keys) {
	checkIsInMulti();
	client.sdiff(keys);
	return BuilderFactory.STRING_SET
		.build(client.getBinaryMultiBulkReply());
    }

    /**
     * This command works exactly like {@link #sdiff(String...) SDIFF} but
     * instead of being returned the resulting set is stored in dstkey.
     * 
     * @param dstkey
     * @param keys
     * @return Status code reply
     */
    public Long sdiffstore(final String dstkey, final String... keys) {
	checkIsInMulti();
	client.sdiffstore(dstkey, keys);
	return client.getIntegerReply();
    }

    /**
     * Return a random element from a Set, without removing the element. If the
     * Set is empty or the key does not exist, a nil object is returned.
     * <p>
     * The SPOP command does a similar work but the returned element is popped
     * (removed) from the Set.
     * <p>
     * Time complexity O(1)
     * 
     * @param key
     * @return Bulk reply
     */
    public String srandmember(final String key) {
	checkIsInMulti();
	client.srandmember(key);
	return client.getBulkReply();
    }

    public List<String> srandmember(final String key, final int count) {
	checkIsInMulti();
	client.srandmember(key, count);
	return client.getMultiBulkReply();
    }

    /**
     * Add the specified member having the specifeid score to the sorted set
     * stored at key. If member is already a member of the sorted set the score
     * is updated, and the element reinserted in the right position to ensure
     * sorting. If key does not exist a new sorted set with the specified member
     * as sole member is crated. If the key exists but does not hold a sorted
     * set value an error is returned.
     * <p>
     * The score value can be the string representation of a double precision
     * floating point number.
     * <p>
     * Time complexity O(log(N)) with N being the number of elements in the
     * sorted set
     * 
     * @param key
     * @param score
     * @param member
     * @return Integer reply, specifically: 1 if the new element was added 0 if
     *         the element was already a member of the sorted set and the score
     *         was updated
     */
    public Long zadd(final String key, final double score, final String member) {
	checkIsInMulti();
	client.zadd(key, score, member);
	return client.getIntegerReply();
    }

    public Long zadd(final String key, final Map<String, Double> scoreMembers) {
	checkIsInMulti();
	client.zadd(key, scoreMembers);
	return client.getIntegerReply();
    }

    public Set<String> zrange(final String key, final long start, final long end) {
	checkIsInMulti();
	client.zrange(key, start, end);
	final List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    /**
     * Remove the specified member from the sorted set value stored at key. If
     * member was not a member of the set no operation is performed. If key does
     * not not hold a set value an error is returned.
     * <p>
     * Time complexity O(log(N)) with N being the number of elements in the
     * sorted set
     * 
     * 
     * 
     * @param key
     * @param members
     * @return Integer reply, specifically: 1 if the new element was removed 0
     *         if the new element was not a member of the set
     */
    public Long zrem(final String key, final String... members) {
	checkIsInMulti();
	client.zrem(key, members);
	return client.getIntegerReply();
    }

    /**
     * If member already exists in the sorted set adds the increment to its
     * score and updates the position of the element in the sorted set
     * accordingly. If member does not already exist in the sorted set it is
     * added with increment as score (that is, like if the previous score was
     * virtually zero). If key does not exist a new sorted set with the
     * specified member as sole member is crated. If the key exists but does not
     * hold a sorted set value an error is returned.
     * <p>
     * The score value can be the string representation of a double precision
     * floating point number. It's possible to provide a negative value to
     * perform a decrement.
     * <p>
     * For an introduction to sorted sets check the Introduction to Redis data
     * types page.
     * <p>
     * Time complexity O(log(N)) with N being the number of elements in the
     * sorted set
     * 
     * @param key
     * @param score
     * @param member
     * @return The new score
     */
    public Double zincrby(final String key, final double score,
	    final String member) {
	checkIsInMulti();
	client.zincrby(key, score, member);
	String newscore = client.getBulkReply();
	return Double.valueOf(newscore);
    }

    /**
     * Return the rank (or index) or member in the sorted set at key, with
     * scores being ordered from low to high.
     * <p>
     * When the given member does not exist in the sorted set, the special value
     * 'nil' is returned. The returned rank (or index) of the member is 0-based
     * for both commands.
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(log(N))
     * 
     * @see #zrevrank(String, String)
     * 
     * @param key
     * @param member
     * @return Integer reply or a nil bulk reply, specifically: the rank of the
     *         element as an integer reply if the element exists. A nil bulk
     *         reply if there is no such element.
     */
    public Long zrank(final String key, final String member) {
	checkIsInMulti();
	client.zrank(key, member);
	return client.getIntegerReply();
    }

    /**
     * Return the rank (or index) or member in the sorted set at key, with
     * scores being ordered from high to low.
     * <p>
     * When the given member does not exist in the sorted set, the special value
     * 'nil' is returned. The returned rank (or index) of the member is 0-based
     * for both commands.
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(log(N))
     * 
     * @see #zrank(String, String)
     * 
     * @param key
     * @param member
     * @return Integer reply or a nil bulk reply, specifically: the rank of the
     *         element as an integer reply if the element exists. A nil bulk
     *         reply if there is no such element.
     */
    public Long zrevrank(final String key, final String member) {
	checkIsInMulti();
	client.zrevrank(key, member);
	return client.getIntegerReply();
    }

    public Set<String> zrevrange(final String key, final long start,
	    final long end) {
	checkIsInMulti();
	client.zrevrange(key, start, end);
	final List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    public Set<Tuple> zrangeWithScores(final String key, final long start,
	    final long end) {
	checkIsInMulti();
	client.zrangeWithScores(key, start, end);
	Set<Tuple> set = getTupledSet();
	return set;
    }

    public Set<Tuple> zrevrangeWithScores(final String key, final long start,
	    final long end) {
	checkIsInMulti();
	client.zrevrangeWithScores(key, start, end);
	Set<Tuple> set = getTupledSet();
	return set;
    }

    /**
     * Return the sorted set cardinality (number of elements). If the key does
     * not exist 0 is returned, like for empty sorted sets.
     * <p>
     * Time complexity O(1)
     * 
     * @param key
     * @return the cardinality (number of elements) of the set as an integer.
     */
    public Long zcard(final String key) {
	checkIsInMulti();
	client.zcard(key);
	return client.getIntegerReply();
    }

    /**
     * Return the score of the specified element of the sorted set at key. If
     * the specified element does not exist in the sorted set, or the key does
     * not exist at all, a special 'nil' value is returned.
     * <p>
     * <b>Time complexity:</b> O(1)
     * 
     * @param key
     * @param member
     * @return the score
     */
    public Double zscore(final String key, final String member) {
	checkIsInMulti();
	client.zscore(key, member);
	final String score = client.getBulkReply();
	return (score != null ? new Double(score) : null);
    }

    public String watch(final String... keys) {
	client.watch(keys);
	return client.getStatusCodeReply();
    }

    /**
     * Sort a Set or a List.
     * <p>
     * Sort the elements contained in the List, Set, or Sorted Set value at key.
     * By default sorting is numeric with elements being compared as double
     * precision floating point numbers. This is the simplest form of SORT.
     * 
     * @see #sort(String, String)
     * @see #sort(String, SortingParams)
     * @see #sort(String, SortingParams, String)
     * 
     * 
     * @param key
     * @return Assuming the Set/List at key contains a list of numbers, the
     *         return value will be the list of numbers ordered from the
     *         smallest to the biggest number.
     */
    public List<String> sort(final String key) {
	checkIsInMulti();
	client.sort(key);
	return client.getMultiBulkReply();
    }

    /**
     * Sort a Set or a List accordingly to the specified parameters.
     * <p>
     * <b>examples:</b>
     * <p>
     * Given are the following sets and key/values:
     * 
     * <pre>
     * x = [1, 2, 3]
     * y = [a, b, c]
     * 
     * k1 = z
     * k2 = y
     * k3 = x
     * 
     * w1 = 9
     * w2 = 8
     * w3 = 7
     * </pre>
     * 
     * Sort Order:
     * 
     * <pre>
     * sort(x) or sort(x, sp.asc())
     * -> [1, 2, 3]
     * 
     * sort(x, sp.desc())
     * -> [3, 2, 1]
     * 
     * sort(y)
     * -> [c, a, b]
     * 
     * sort(y, sp.alpha())
     * -> [a, b, c]
     * 
     * sort(y, sp.alpha().desc())
     * -> [c, a, b]
     * </pre>
     * 
     * Limit (e.g. for Pagination):
     * 
     * <pre>
     * sort(x, sp.limit(0, 2))
     * -> [1, 2]
     * 
     * sort(y, sp.alpha().desc().limit(1, 2))
     * -> [b, a]
     * </pre>
     * 
     * Sorting by external keys:
     * 
     * <pre>
     * sort(x, sb.by(w*))
     * -> [3, 2, 1]
     * 
     * sort(x, sb.by(w*).desc())
     * -> [1, 2, 3]
     * </pre>
     * 
     * Getting external keys:
     * 
     * <pre>
     * sort(x, sp.by(w*).get(k*))
     * -> [x, y, z]
     * 
     * sort(x, sp.by(w*).get(#).get(k*))
     * -> [3, x, 2, y, 1, z]
     * </pre>
     * 
     * @see #sort(String)
     * @see #sort(String, SortingParams, String)
     * 
     * @param key
     * @param sortingParameters
     * @return a list of sorted elements.
     */
    public List<String> sort(final String key,
	    final SortingParams sortingParameters) {
	checkIsInMulti();
	client.sort(key, sortingParameters);
	return client.getMultiBulkReply();
    }

    /**
     * BLPOP (and BRPOP) is a blocking list pop primitive. You can see this
     * commands as blocking versions of LPOP and RPOP able to block if the
     * specified keys don't exist or contain empty lists.
     * <p>
     * The following is a description of the exact semantic. We describe BLPOP
     * but the two commands are identical, the only difference is that BLPOP
     * pops the element from the left (head) of the list, and BRPOP pops from
     * the right (tail).
     * <p>
     * <b>Non blocking behavior</b>
     * <p>
     * When BLPOP is called, if at least one of the specified keys contain a non
     * empty list, an element is popped from the head of the list and returned
     * to the caller together with the name of the key (BLPOP returns a two
     * elements array, the first element is the key, the second the popped
     * value).
     * <p>
     * Keys are scanned from left to right, so for instance if you issue BLPOP
     * list1 list2 list3 0 against a dataset where list1 does not exist but
     * list2 and list3 contain non empty lists, BLPOP guarantees to return an
     * element from the list stored at list2 (since it is the first non empty
     * list starting from the left).
     * <p>
     * <b>Blocking behavior</b>
     * <p>
     * If none of the specified keys exist or contain non empty lists, BLPOP
     * blocks until some other client performs a LPUSH or an RPUSH operation
     * against one of the lists.
     * <p>
     * Once new data is present on one of the lists, the client finally returns
     * with the name of the key unblocking it and the popped value.
     * <p>
     * When blocking, if a non-zero timeout is specified, the client will
     * unblock returning a nil special value if the specified amount of seconds
     * passed without a push operation against at least one of the specified
     * keys.
     * <p>
     * The timeout argument is interpreted as an integer value. A timeout of
     * zero means instead to block forever.
     * <p>
     * <b>Multiple clients blocking for the same keys</b>
     * <p>
     * Multiple clients can block for the same key. They are put into a queue,
     * so the first to be served will be the one that started to wait earlier,
     * in a first-blpopping first-served fashion.
     * <p>
     * <b>blocking POP inside a MULTI/EXEC transaction</b>
     * <p>
     * BLPOP and BRPOP can be used with pipelining (sending multiple commands
     * and reading the replies in batch), but it does not make sense to use
     * BLPOP or BRPOP inside a MULTI/EXEC block (a Redis transaction).
     * <p>
     * The behavior of BLPOP inside MULTI/EXEC when the list is empty is to
     * return a multi-bulk nil reply, exactly what happens when the timeout is
     * reached. If you like science fiction, think at it like if inside
     * MULTI/EXEC the time will flow at infinite speed :)
     * <p>
     * Time complexity: O(1)
     * 
     * @see #brpop(int, String...)
     * 
     * @param timeout
     * @param keys
     * @return BLPOP returns a two-elements array via a multi bulk reply in
     *         order to return both the unblocking key and the popped value.
     *         <p>
     *         When a non-zero timeout is specified, and the BLPOP operation
     *         timed out, the return value is a nil multi bulk reply. Most
     *         client values will return false or nil accordingly to the
     *         programming language used.
     */
    public List<String> blpop(final int timeout, final String... keys) {
	checkIsInMulti();
	List<String> args = new ArrayList<String>();
	for (String arg : keys) {
	    args.add(arg);
	}
	args.add(String.valueOf(timeout));

	client.blpop(args.toArray(new String[args.size()]));
	client.setTimeoutInfinite();
	final List<String> multiBulkReply = client.getMultiBulkReply();
	client.rollbackTimeout();
	return multiBulkReply;
    }

    public List<String> blpop(String... args) {
	client.blpop(args);
	client.setTimeoutInfinite();
	final List<String> multiBulkReply = client.getMultiBulkReply();
	client.rollbackTimeout();
	return multiBulkReply;
    }

    public List<String> brpop(String... args) {
	client.brpop(args);
	client.setTimeoutInfinite();
	final List<String> multiBulkReply = client.getMultiBulkReply();
	client.rollbackTimeout();
	return multiBulkReply;
    }

    public List<String> blpop(String arg) {
	String[] args = new String[1];
	args[0] = arg;
	client.blpop(args);
	client.setTimeoutInfinite();
	final List<String> multiBulkReply = client.getMultiBulkReply();
	client.rollbackTimeout();
	return multiBulkReply;
    }

    public List<String> brpop(String arg) {
	String[] args = new String[1];
	args[0] = arg;
	client.brpop(args);
	client.setTimeoutInfinite();
	final List<String> multiBulkReply = client.getMultiBulkReply();
	client.rollbackTimeout();
	return multiBulkReply;
    }

    /**
     * Sort a Set or a List accordingly to the specified parameters and store
     * the result at dstkey.
     * 
     * @see #sort(String, SortingParams)
     * @see #sort(String)
     * @see #sort(String, String)
     * 
     * @param key
     * @param sortingParameters
     * @param dstkey
     * @return The number of elements of the list at dstkey.
     */
    public Long sort(final String key, final SortingParams sortingParameters,
	    final String dstkey) {
	checkIsInMulti();
	client.sort(key, sortingParameters, dstkey);
	return client.getIntegerReply();
    }

    /**
     * Sort a Set or a List and Store the Result at dstkey.
     * <p>
     * Sort the elements contained in the List, Set, or Sorted Set value at key
     * and store the result at dstkey. By default sorting is numeric with
     * elements being compared as double precision floating point numbers. This
     * is the simplest form of SORT.
     * 
     * @see #sort(String)
     * @see #sort(String, SortingParams)
     * @see #sort(String, SortingParams, String)
     * 
     * @param key
     * @param dstkey
     * @return The number of elements of the list at dstkey.
     */
    public Long sort(final String key, final String dstkey) {
	checkIsInMulti();
	client.sort(key, dstkey);
	return client.getIntegerReply();
    }

    /**
     * BLPOP (and BRPOP) is a blocking list pop primitive. You can see this
     * commands as blocking versions of LPOP and RPOP able to block if the
     * specified keys don't exist or contain empty lists.
     * <p>
     * The following is a description of the exact semantic. We describe BLPOP
     * but the two commands are identical, the only difference is that BLPOP
     * pops the element from the left (head) of the list, and BRPOP pops from
     * the right (tail).
     * <p>
     * <b>Non blocking behavior</b>
     * <p>
     * When BLPOP is called, if at least one of the specified keys contain a non
     * empty list, an element is popped from the head of the list and returned
     * to the caller together with the name of the key (BLPOP returns a two
     * elements array, the first element is the key, the second the popped
     * value).
     * <p>
     * Keys are scanned from left to right, so for instance if you issue BLPOP
     * list1 list2 list3 0 against a dataset where list1 does not exist but
     * list2 and list3 contain non empty lists, BLPOP guarantees to return an
     * element from the list stored at list2 (since it is the first non empty
     * list starting from the left).
     * <p>
     * <b>Blocking behavior</b>
     * <p>
     * If none of the specified keys exist or contain non empty lists, BLPOP
     * blocks until some other client performs a LPUSH or an RPUSH operation
     * against one of the lists.
     * <p>
     * Once new data is present on one of the lists, the client finally returns
     * with the name of the key unblocking it and the popped value.
     * <p>
     * When blocking, if a non-zero timeout is specified, the client will
     * unblock returning a nil special value if the specified amount of seconds
     * passed without a push operation against at least one of the specified
     * keys.
     * <p>
     * The timeout argument is interpreted as an integer value. A timeout of
     * zero means instead to block forever.
     * <p>
     * <b>Multiple clients blocking for the same keys</b>
     * <p>
     * Multiple clients can block for the same key. They are put into a queue,
     * so the first to be served will be the one that started to wait earlier,
     * in a first-blpopping first-served fashion.
     * <p>
     * <b>blocking POP inside a MULTI/EXEC transaction</b>
     * <p>
     * BLPOP and BRPOP can be used with pipelining (sending multiple commands
     * and reading the replies in batch), but it does not make sense to use
     * BLPOP or BRPOP inside a MULTI/EXEC block (a Redis transaction).
     * <p>
     * The behavior of BLPOP inside MULTI/EXEC when the list is empty is to
     * return a multi-bulk nil reply, exactly what happens when the timeout is
     * reached. If you like science fiction, think at it like if inside
     * MULTI/EXEC the time will flow at infinite speed :)
     * <p>
     * Time complexity: O(1)
     * 
     * @see #blpop(int, String...)
     * 
     * @param timeout
     * @param keys
     * @return BLPOP returns a two-elements array via a multi bulk reply in
     *         order to return both the unblocking key and the popped value.
     *         <p>
     *         When a non-zero timeout is specified, and the BLPOP operation
     *         timed out, the return value is a nil multi bulk reply. Most
     *         client values will return false or nil accordingly to the
     *         programming language used.
     */
    public List<String> brpop(final int timeout, final String... keys) {
	checkIsInMulti();
	List<String> args = new ArrayList<String>();
	for (String arg : keys) {
	    args.add(arg);
	}
	args.add(String.valueOf(timeout));

	client.brpop(args.toArray(new String[args.size()]));
	client.setTimeoutInfinite();
	List<String> multiBulkReply = client.getMultiBulkReply();
	client.rollbackTimeout();

	return multiBulkReply;
    }

    public Long zcount(final String key, final double min, final double max) {
	checkIsInMulti();
	client.zcount(key, min, max);
	return client.getIntegerReply();
    }

    public Long zcount(final String key, final String min, final String max) {
	checkIsInMulti();
	client.zcount(key, min, max);
	return client.getIntegerReply();
    }

    /**
     * Return the all the elements in the sorted set at key with a score between
     * min and max (including elements with score equal to min or max).
     * <p>
     * The elements having the same score are returned sorted lexicographically
     * as ASCII strings (this follows from a property of Redis sorted sets and
     * does not involve further computation).
     * <p>
     * Using the optional
     * {@link #zrangeByScore(String, double, double, int, int) LIMIT} it's
     * possible to get only a range of the matching elements in an SQL-alike
     * way. Note that if offset is large the commands needs to traverse the list
     * for offset elements and this adds up to the O(M) figure.
     * <p>
     * The {@link #zcount(String, double, double) ZCOUNT} command is similar to
     * {@link #zrangeByScore(String, double, double) ZRANGEBYSCORE} but instead
     * of returning the actual elements in the specified interval, it just
     * returns the number of matching elements.
     * <p>
     * <b>Exclusive intervals and infinity</b>
     * <p>
     * min and max can be -inf and +inf, so that you are not required to know
     * what's the greatest or smallest element in order to take, for instance,
     * elements "up to a given value".
     * <p>
     * Also while the interval is for default closed (inclusive) it's possible
     * to specify open intervals prefixing the score with a "(" character, so
     * for instance:
     * <p>
     * {@code ZRANGEBYSCORE zset (1.3 5}
     * <p>
     * Will return all the values with score > 1.3 and <= 5, while for instance:
     * <p>
     * {@code ZRANGEBYSCORE zset (5 (10}
     * <p>
     * Will return all the values with score > 5 and < 10 (5 and 10 excluded).
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(log(N))+O(M) with N being the number of elements in the sorted set and
     * M the number of elements returned by the command, so if M is constant
     * (for instance you always ask for the first ten elements with LIMIT) you
     * can consider it O(log(N))
     * 
     * @see #zrangeByScore(String, double, double)
     * @see #zrangeByScore(String, double, double, int, int)
     * @see #zrangeByScoreWithScores(String, double, double)
     * @see #zrangeByScoreWithScores(String, String, String)
     * @see #zrangeByScoreWithScores(String, double, double, int, int)
     * @see #zcount(String, double, double)
     * 
     * @param key
     * @param min
     *            a double or Double.MIN_VALUE for "-inf"
     * @param max
     *            a double or Double.MAX_VALUE for "+inf"
     * @return Multi bulk reply specifically a list of elements in the specified
     *         score range.
     */
    public Set<String> zrangeByScore(final String key, final double min,
	    final double max) {
	checkIsInMulti();
	client.zrangeByScore(key, min, max);
	return new LinkedHashSet<String>(client.getMultiBulkReply());
    }

    public Set<String> zrangeByScore(final String key, final String min,
	    final String max) {
	checkIsInMulti();
	client.zrangeByScore(key, min, max);
	return new LinkedHashSet<String>(client.getMultiBulkReply());
    }

    /**
     * Return the all the elements in the sorted set at key with a score between
     * min and max (including elements with score equal to min or max).
     * <p>
     * The elements having the same score are returned sorted lexicographically
     * as ASCII strings (this follows from a property of Redis sorted sets and
     * does not involve further computation).
     * <p>
     * Using the optional
     * {@link #zrangeByScore(String, double, double, int, int) LIMIT} it's
     * possible to get only a range of the matching elements in an SQL-alike
     * way. Note that if offset is large the commands needs to traverse the list
     * for offset elements and this adds up to the O(M) figure.
     * <p>
     * The {@link #zcount(String, double, double) ZCOUNT} command is similar to
     * {@link #zrangeByScore(String, double, double) ZRANGEBYSCORE} but instead
     * of returning the actual elements in the specified interval, it just
     * returns the number of matching elements.
     * <p>
     * <b>Exclusive intervals and infinity</b>
     * <p>
     * min and max can be -inf and +inf, so that you are not required to know
     * what's the greatest or smallest element in order to take, for instance,
     * elements "up to a given value".
     * <p>
     * Also while the interval is for default closed (inclusive) it's possible
     * to specify open intervals prefixing the score with a "(" character, so
     * for instance:
     * <p>
     * {@code ZRANGEBYSCORE zset (1.3 5}
     * <p>
     * Will return all the values with score > 1.3 and <= 5, while for instance:
     * <p>
     * {@code ZRANGEBYSCORE zset (5 (10}
     * <p>
     * Will return all the values with score > 5 and < 10 (5 and 10 excluded).
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(log(N))+O(M) with N being the number of elements in the sorted set and
     * M the number of elements returned by the command, so if M is constant
     * (for instance you always ask for the first ten elements with LIMIT) you
     * can consider it O(log(N))
     * 
     * @see #zrangeByScore(String, double, double)
     * @see #zrangeByScore(String, double, double, int, int)
     * @see #zrangeByScoreWithScores(String, double, double)
     * @see #zrangeByScoreWithScores(String, double, double, int, int)
     * @see #zcount(String, double, double)
     * 
     * @param key
     * @param min
     * @param max
     * @return Multi bulk reply specifically a list of elements in the specified
     *         score range.
     */
    public Set<String> zrangeByScore(final String key, final double min,
	    final double max, final int offset, final int count) {
	checkIsInMulti();
	client.zrangeByScore(key, min, max, offset, count);
	return new LinkedHashSet<String>(client.getMultiBulkReply());
    }

    public Set<String> zrangeByScore(final String key, final String min,
	    final String max, final int offset, final int count) {
	checkIsInMulti();
	client.zrangeByScore(key, min, max, offset, count);
	return new LinkedHashSet<String>(client.getMultiBulkReply());
    }

    /**
     * Return the all the elements in the sorted set at key with a score between
     * min and max (including elements with score equal to min or max).
     * <p>
     * The elements having the same score are returned sorted lexicographically
     * as ASCII strings (this follows from a property of Redis sorted sets and
     * does not involve further computation).
     * <p>
     * Using the optional
     * {@link #zrangeByScore(String, double, double, int, int) LIMIT} it's
     * possible to get only a range of the matching elements in an SQL-alike
     * way. Note that if offset is large the commands needs to traverse the list
     * for offset elements and this adds up to the O(M) figure.
     * <p>
     * The {@link #zcount(String, double, double) ZCOUNT} command is similar to
     * {@link #zrangeByScore(String, double, double) ZRANGEBYSCORE} but instead
     * of returning the actual elements in the specified interval, it just
     * returns the number of matching elements.
     * <p>
     * <b>Exclusive intervals and infinity</b>
     * <p>
     * min and max can be -inf and +inf, so that you are not required to know
     * what's the greatest or smallest element in order to take, for instance,
     * elements "up to a given value".
     * <p>
     * Also while the interval is for default closed (inclusive) it's possible
     * to specify open intervals prefixing the score with a "(" character, so
     * for instance:
     * <p>
     * {@code ZRANGEBYSCORE zset (1.3 5}
     * <p>
     * Will return all the values with score > 1.3 and <= 5, while for instance:
     * <p>
     * {@code ZRANGEBYSCORE zset (5 (10}
     * <p>
     * Will return all the values with score > 5 and < 10 (5 and 10 excluded).
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(log(N))+O(M) with N being the number of elements in the sorted set and
     * M the number of elements returned by the command, so if M is constant
     * (for instance you always ask for the first ten elements with LIMIT) you
     * can consider it O(log(N))
     * 
     * @see #zrangeByScore(String, double, double)
     * @see #zrangeByScore(String, double, double, int, int)
     * @see #zrangeByScoreWithScores(String, double, double)
     * @see #zrangeByScoreWithScores(String, double, double, int, int)
     * @see #zcount(String, double, double)
     * 
     * @param key
     * @param min
     * @param max
     * @return Multi bulk reply specifically a list of elements in the specified
     *         score range.
     */
    public Set<Tuple> zrangeByScoreWithScores(final String key,
	    final double min, final double max) {
	checkIsInMulti();
	client.zrangeByScoreWithScores(key, min, max);
	Set<Tuple> set = getTupledSet();
	return set;
    }

    public Set<Tuple> zrangeByScoreWithScores(final String key,
	    final String min, final String max) {
	checkIsInMulti();
	client.zrangeByScoreWithScores(key, min, max);
	Set<Tuple> set = getTupledSet();
	return set;
    }

    /**
     * Return the all the elements in the sorted set at key with a score between
     * min and max (including elements with score equal to min or max).
     * <p>
     * The elements having the same score are returned sorted lexicographically
     * as ASCII strings (this follows from a property of Redis sorted sets and
     * does not involve further computation).
     * <p>
     * Using the optional
     * {@link #zrangeByScore(String, double, double, int, int) LIMIT} it's
     * possible to get only a range of the matching elements in an SQL-alike
     * way. Note that if offset is large the commands needs to traverse the list
     * for offset elements and this adds up to the O(M) figure.
     * <p>
     * The {@link #zcount(String, double, double) ZCOUNT} command is similar to
     * {@link #zrangeByScore(String, double, double) ZRANGEBYSCORE} but instead
     * of returning the actual elements in the specified interval, it just
     * returns the number of matching elements.
     * <p>
     * <b>Exclusive intervals and infinity</b>
     * <p>
     * min and max can be -inf and +inf, so that you are not required to know
     * what's the greatest or smallest element in order to take, for instance,
     * elements "up to a given value".
     * <p>
     * Also while the interval is for default closed (inclusive) it's possible
     * to specify open intervals prefixing the score with a "(" character, so
     * for instance:
     * <p>
     * {@code ZRANGEBYSCORE zset (1.3 5}
     * <p>
     * Will return all the values with score > 1.3 and <= 5, while for instance:
     * <p>
     * {@code ZRANGEBYSCORE zset (5 (10}
     * <p>
     * Will return all the values with score > 5 and < 10 (5 and 10 excluded).
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(log(N))+O(M) with N being the number of elements in the sorted set and
     * M the number of elements returned by the command, so if M is constant
     * (for instance you always ask for the first ten elements with LIMIT) you
     * can consider it O(log(N))
     * 
     * @see #zrangeByScore(String, double, double)
     * @see #zrangeByScore(String, double, double, int, int)
     * @see #zrangeByScoreWithScores(String, double, double)
     * @see #zrangeByScoreWithScores(String, double, double, int, int)
     * @see #zcount(String, double, double)
     * 
     * @param key
     * @param min
     * @param max
     * @return Multi bulk reply specifically a list of elements in the specified
     *         score range.
     */
    public Set<Tuple> zrangeByScoreWithScores(final String key,
	    final double min, final double max, final int offset,
	    final int count) {
	checkIsInMulti();
	client.zrangeByScoreWithScores(key, min, max, offset, count);
	Set<Tuple> set = getTupledSet();
	return set;
    }

    public Set<Tuple> zrangeByScoreWithScores(final String key,
	    final String min, final String max, final int offset,
	    final int count) {
	checkIsInMulti();
	client.zrangeByScoreWithScores(key, min, max, offset, count);
	Set<Tuple> set = getTupledSet();
	return set;
    }

    private Set<Tuple> getTupledSet() {
	checkIsInMulti();
	List<String> membersWithScores = client.getMultiBulkReply();
	Set<Tuple> set = new LinkedHashSet<Tuple>();
	Iterator<String> iterator = membersWithScores.iterator();
	while (iterator.hasNext()) {
	    set.add(new Tuple(iterator.next(), Double.valueOf(iterator.next())));
	}
	return set;
    }

    public Set<String> zrevrangeByScore(final String key, final double max,
	    final double min) {
	checkIsInMulti();
	client.zrevrangeByScore(key, max, min);
	return new LinkedHashSet<String>(client.getMultiBulkReply());
    }

    public Set<String> zrevrangeByScore(final String key, final String max,
	    final String min) {
	checkIsInMulti();
	client.zrevrangeByScore(key, max, min);
	return new LinkedHashSet<String>(client.getMultiBulkReply());
    }

    public Set<String> zrevrangeByScore(final String key, final double max,
	    final double min, final int offset, final int count) {
	checkIsInMulti();
	client.zrevrangeByScore(key, max, min, offset, count);
	return new LinkedHashSet<String>(client.getMultiBulkReply());
    }

    public Set<Tuple> zrevrangeByScoreWithScores(final String key,
	    final double max, final double min) {
	checkIsInMulti();
	client.zrevrangeByScoreWithScores(key, max, min);
	Set<Tuple> set = getTupledSet();
	return set;
    }

    public Set<Tuple> zrevrangeByScoreWithScores(final String key,
	    final double max, final double min, final int offset,
	    final int count) {
	checkIsInMulti();
	client.zrevrangeByScoreWithScores(key, max, min, offset, count);
	Set<Tuple> set = getTupledSet();
	return set;
    }

    public Set<Tuple> zrevrangeByScoreWithScores(final String key,
	    final String max, final String min, final int offset,
	    final int count) {
	checkIsInMulti();
	client.zrevrangeByScoreWithScores(key, max, min, offset, count);
	Set<Tuple> set = getTupledSet();
	return set;
    }

    public Set<String> zrevrangeByScore(final String key, final String max,
	    final String min, final int offset, final int count) {
	checkIsInMulti();
	client.zrevrangeByScore(key, max, min, offset, count);
	return new LinkedHashSet<String>(client.getMultiBulkReply());
    }

    public Set<Tuple> zrevrangeByScoreWithScores(final String key,
	    final String max, final String min) {
	checkIsInMulti();
	client.zrevrangeByScoreWithScores(key, max, min);
	Set<Tuple> set = getTupledSet();
	return set;
    }

    /**
     * Remove all elements in the sorted set at key with rank between start and
     * end. Start and end are 0-based with rank 0 being the element with the
     * lowest score. Both start and end can be negative numbers, where they
     * indicate offsets starting at the element with the highest rank. For
     * example: -1 is the element with the highest score, -2 the element with
     * the second highest score and so forth.
     * <p>
     * <b>Time complexity:</b> O(log(N))+O(M) with N being the number of
     * elements in the sorted set and M the number of elements removed by the
     * operation
     * 
     */
    public Long zremrangeByRank(final String key, final long start,
	    final long end) {
	checkIsInMulti();
	client.zremrangeByRank(key, start, end);
	return client.getIntegerReply();
    }

    /**
     * Remove all the elements in the sorted set at key with a score between min
     * and max (including elements with score equal to min or max).
     * <p>
     * <b>Time complexity:</b>
     * <p>
     * O(log(N))+O(M) with N being the number of elements in the sorted set and
     * M the number of elements removed by the operation
     * 
     * @param key
     * @param start
     * @param end
     * @return Integer reply, specifically the number of elements removed.
     */
    public Long zremrangeByScore(final String key, final double start,
	    final double end) {
	checkIsInMulti();
	client.zremrangeByScore(key, start, end);
	return client.getIntegerReply();
    }

    public Long zremrangeByScore(final String key, final String start,
	    final String end) {
	checkIsInMulti();
	client.zremrangeByScore(key, start, end);
	return client.getIntegerReply();
    }

    /**
     * Creates a union or intersection of N sorted sets given by keys k1 through
     * kN, and stores it at dstkey. It is mandatory to provide the number of
     * input keys N, before passing the input keys and the other (optional)
     * arguments.
     * <p>
     * As the terms imply, the {@link #zinterstore(String, String...)
     * ZINTERSTORE} command requires an element to be present in each of the
     * given inputs to be inserted in the result. The
     * {@link #zunionstore(String, String...) ZUNIONSTORE} command inserts all
     * elements across all inputs.
     * <p>
     * Using the WEIGHTS option, it is possible to add weight to each input
     * sorted set. This means that the score of each element in the sorted set
     * is first multiplied by this weight before being passed to the
     * aggregation. When this option is not given, all weights default to 1.
     * <p>
     * With the AGGREGATE option, it's possible to specify how the results of
     * the union or intersection are aggregated. This option defaults to SUM,
     * where the score of an element is summed across the inputs where it
     * exists. When this option is set to be either MIN or MAX, the resulting
     * set will contain the minimum or maximum score of an element across the
     * inputs where it exists.
     * <p>
     * <b>Time complexity:</b> O(N) + O(M log(M)) with N being the sum of the
     * sizes of the input sorted sets, and M being the number of elements in the
     * resulting sorted set
     * 
     * @see #zunionstore(String, String...)
     * @see #zunionstore(String, ZParams, String...)
     * @see #zinterstore(String, String...)
     * @see #zinterstore(String, ZParams, String...)
     * 
     * @param dstkey
     * @param sets
     * @return Integer reply, specifically the number of elements in the sorted
     *         set at dstkey
     */
    public Long zunionstore(final String dstkey, final String... sets) {
	checkIsInMulti();
	client.zunionstore(dstkey, sets);
	return client.getIntegerReply();
    }

    /**
     * Creates a union or intersection of N sorted sets given by keys k1 through
     * kN, and stores it at dstkey. It is mandatory to provide the number of
     * input keys N, before passing the input keys and the other (optional)
     * arguments.
     * <p>
     * As the terms imply, the {@link #zinterstore(String, String...)
     * ZINTERSTORE} command requires an element to be present in each of the
     * given inputs to be inserted in the result. The
     * {@link #zunionstore(String, String...) ZUNIONSTORE} command inserts all
     * elements across all inputs.
     * <p>
     * Using the WEIGHTS option, it is possible to add weight to each input
     * sorted set. This means that the score of each element in the sorted set
     * is first multiplied by this weight before being passed to the
     * aggregation. When this option is not given, all weights default to 1.
     * <p>
     * With the AGGREGATE option, it's possible to specify how the results of
     * the union or intersection are aggregated. This option defaults to SUM,
     * where the score of an element is summed across the inputs where it
     * exists. When this option is set to be either MIN or MAX, the resulting
     * set will contain the minimum or maximum score of an element across the
     * inputs where it exists.
     * <p>
     * <b>Time complexity:</b> O(N) + O(M log(M)) with N being the sum of the
     * sizes of the input sorted sets, and M being the number of elements in the
     * resulting sorted set
     * 
     * @see #zunionstore(String, String...)
     * @see #zunionstore(String, ZParams, String...)
     * @see #zinterstore(String, String...)
     * @see #zinterstore(String, ZParams, String...)
     * 
     * @param dstkey
     * @param sets
     * @param params
     * @return Integer reply, specifically the number of elements in the sorted
     *         set at dstkey
     */
    public Long zunionstore(final String dstkey, final ZParams params,
	    final String... sets) {
	checkIsInMulti();
	client.zunionstore(dstkey, params, sets);
	return client.getIntegerReply();
    }

    /**
     * Creates a union or intersection of N sorted sets given by keys k1 through
     * kN, and stores it at dstkey. It is mandatory to provide the number of
     * input keys N, before passing the input keys and the other (optional)
     * arguments.
     * <p>
     * As the terms imply, the {@link #zinterstore(String, String...)
     * ZINTERSTORE} command requires an element to be present in each of the
     * given inputs to be inserted in the result. The
     * {@link #zunionstore(String, String...) ZUNIONSTORE} command inserts all
     * elements across all inputs.
     * <p>
     * Using the WEIGHTS option, it is possible to add weight to each input
     * sorted set. This means that the score of each element in the sorted set
     * is first multiplied by this weight before being passed to the
     * aggregation. When this option is not given, all weights default to 1.
     * <p>
     * With the AGGREGATE option, it's possible to specify how the results of
     * the union or intersection are aggregated. This option defaults to SUM,
     * where the score of an element is summed across the inputs where it
     * exists. When this option is set to be either MIN or MAX, the resulting
     * set will contain the minimum or maximum score of an element across the
     * inputs where it exists.
     * <p>
     * <b>Time complexity:</b> O(N) + O(M log(M)) with N being the sum of the
     * sizes of the input sorted sets, and M being the number of elements in the
     * resulting sorted set
     * 
     * @see #zunionstore(String, String...)
     * @see #zunionstore(String, ZParams, String...)
     * @see #zinterstore(String, String...)
     * @see #zinterstore(String, ZParams, String...)
     * 
     * @param dstkey
     * @param sets
     * @return Integer reply, specifically the number of elements in the sorted
     *         set at dstkey
     */
    public Long zinterstore(final String dstkey, final String... sets) {
	checkIsInMulti();
	client.zinterstore(dstkey, sets);
	return client.getIntegerReply();
    }

    /**
     * Creates a union or intersection of N sorted sets given by keys k1 through
     * kN, and stores it at dstkey. It is mandatory to provide the number of
     * input keys N, before passing the input keys and the other (optional)
     * arguments.
     * <p>
     * As the terms imply, the {@link #zinterstore(String, String...)
     * ZINTERSTORE} command requires an element to be present in each of the
     * given inputs to be inserted in the result. The
     * {@link #zunionstore(String, String...) ZUNIONSTORE} command inserts all
     * elements across all inputs.
     * <p>
     * Using the WEIGHTS option, it is possible to add weight to each input
     * sorted set. This means that the score of each element in the sorted set
     * is first multiplied by this weight before being passed to the
     * aggregation. When this option is not given, all weights default to 1.
     * <p>
     * With the AGGREGATE option, it's possible to specify how the results of
     * the union or intersection are aggregated. This option defaults to SUM,
     * where the score of an element is summed across the inputs where it
     * exists. When this option is set to be either MIN or MAX, the resulting
     * set will contain the minimum or maximum score of an element across the
     * inputs where it exists.
     * <p>
     * <b>Time complexity:</b> O(N) + O(M log(M)) with N being the sum of the
     * sizes of the input sorted sets, and M being the number of elements in the
     * resulting sorted set
     * 
     * @see #zunionstore(String, String...)
     * @see #zunionstore(String, ZParams, String...)
     * @see #zinterstore(String, String...)
     * @see #zinterstore(String, ZParams, String...)
     * 
     * @param dstkey
     * @param sets
     * @param params
     * @return Integer reply, specifically the number of elements in the sorted
     *         set at dstkey
     */
    public Long zinterstore(final String dstkey, final ZParams params,
	    final String... sets) {
	checkIsInMulti();
	client.zinterstore(dstkey, params, sets);
	return client.getIntegerReply();
    }

    @Override
    public Long zlexcount(final String key, final String min, final String max) {
	checkIsInMulti();
	client.zlexcount(key, min, max);
	return client.getIntegerReply();
    }

    @Override
    public Set<String> zrangeByLex(final String key, final String min,
	    final String max) {
	checkIsInMulti();
	client.zrangeByLex(key, min, max);
	return new LinkedHashSet<String>(client.getMultiBulkReply());
    }

    @Override
    public Set<String> zrangeByLex(final String key, final String min,
	    final String max, final int offset, final int count) {
	checkIsInMulti();
	client.zrangeByLex(key, min, max, offset, count);
	return new LinkedHashSet<String>(client.getMultiBulkReply());
    }

    @Override
    public Set<String> zrevrangeByLex(String key, String max, String min) {
	checkIsInMulti();
	client.zrevrangeByLex(key, max, min);
	return new LinkedHashSet<String>(client.getMultiBulkReply());
    }

    @Override
    public Set<String> zrevrangeByLex(String key, String max, String min,
	    int offset, int count) {
	checkIsInMulti();
	client.zrevrangeByLex(key, max, min, offset, count);
	return new LinkedHashSet<String>(client.getMultiBulkReply());
    }

    @Override
    public Long zremrangeByLex(final String key, final String min,
	    final String max) {
	checkIsInMulti();
	client.zremrangeByLex(key, min, max);
	return client.getIntegerReply();
    }

    public Long strlen(final String key) {
	client.strlen(key);
	return client.getIntegerReply();
    }

    public Long lpushx(final String key, final String... string) {
	client.lpushx(key, string);
	return client.getIntegerReply();
    }

    /**
     * Undo a {@link #expire(String, int) expire} at turning the expire key into
     * a normal key.
     * <p>
     * Time complexity: O(1)
     * 
     * @param key
     * @return Integer reply, specifically: 1: the key is now persist. 0: the
     *         key is not persist (only happens when key not set).
     */
    public Long persist(final String key) {
	client.persist(key);
	return client.getIntegerReply();
    }

    public Long rpushx(final String key, final String... string) {
	client.rpushx(key, string);
	return client.getIntegerReply();
    }

    public String echo(final String string) {
	client.echo(string);
	return client.getBulkReply();
    }

    public Long linsert(final String key, final LIST_POSITION where,
	    final String pivot, final String value) {
	client.linsert(key, where, pivot, value);
	return client.getIntegerReply();
    }

    /**
     * Pop a value from a list, push it to another list and return it; or block
     * until one is available
     * 
     * @param source
     * @param destination
     * @param timeout
     * @return the element
     */
    public String brpoplpush(String source, String destination, int timeout) {
	client.brpoplpush(source, destination, timeout);
	client.setTimeoutInfinite();
	String reply = client.getBulkReply();
	client.rollbackTimeout();
	return reply;
    }

    /**
     * Sets or clears the bit at offset in the string value stored at key
     * 
     * @param key
     * @param offset
     * @param value
     * @return
     */
    public Boolean setbit(String key, long offset, boolean value) {
	client.setbit(key, offset, value);
	return client.getIntegerReply() == 1;
    }

    public Boolean setbit(String key, long offset, String value) {
	client.setbit(key, offset, value);
	return client.getIntegerReply() == 1;
    }

    /**
     * Returns the bit value at offset in the string value stored at key
     * 
     * @param key
     * @param offset
     * @return
     */
    public Boolean getbit(String key, long offset) {
	client.getbit(key, offset);
	return client.getIntegerReply() == 1;
    }

    public Long setrange(String key, long offset, String value) {
	client.setrange(key, offset, value);
	return client.getIntegerReply();
    }

    public String getrange(String key, long startOffset, long endOffset) {
	client.getrange(key, startOffset, endOffset);
	return client.getBulkReply();
    }

    public Long bitpos(final String key, final boolean value) {
	return bitpos(key, value, new BitPosParams());
    }

    public Long bitpos(final String key, final boolean value,
	    final BitPosParams params) {
	client.bitpos(key, value, params);
	return client.getIntegerReply();
    }

    /**
     * Retrieve the configuration of a running Redis server. Not all the
     * configuration parameters are supported.
     * <p>
     * CONFIG GET returns the current configuration parameters. This sub command
     * only accepts a single argument, that is glob style pattern. All the
     * configuration parameters matching this parameter are reported as a list
     * of key-value pairs.
     * <p>
     * <b>Example:</b>
     * 
     * <pre>
     * $ redis-cli config get '*'
     * 1. "dbfilename"
     * 2. "dump.rdb"
     * 3. "requirepass"
     * 4. (nil)
     * 5. "masterauth"
     * 6. (nil)
     * 7. "maxmemory"
     * 8. "0\n"
     * 9. "appendfsync"
     * 10. "everysec"
     * 11. "save"
     * 12. "3600 1 300 100 60 10000"
     * 
     * $ redis-cli config get 'm*'
     * 1. "masterauth"
     * 2. (nil)
     * 3. "maxmemory"
     * 4. "0\n"
     * </pre>
     * 
     * @param pattern
     * @return Bulk reply.
     */
    public List<String> configGet(final String pattern) {
	client.configGet(pattern);
	return client.getMultiBulkReply();
    }

    /**
     * Alter the configuration of a running Redis server. Not all the
     * configuration parameters are supported.
     * <p>
     * The list of configuration parameters supported by CONFIG SET can be
     * obtained issuing a {@link #configGet(String) CONFIG GET *} command.
     * <p>
     * The configuration set using CONFIG SET is immediately loaded by the Redis
     * server that will start acting as specified starting from the next
     * command.
     * <p>
     * 
     * <b>Parameters value format</b>
     * <p>
     * The value of the configuration parameter is the same as the one of the
     * same parameter in the Redis configuration file, with the following
     * exceptions:
     * <p>
     * <ul>
     * <li>The save paramter is a list of space-separated integers. Every pair
     * of integers specify the time and number of changes limit to trigger a
     * save. For instance the command CONFIG SET save "3600 10 60 10000" will
     * configure the server to issue a background saving of the RDB file every
     * 3600 seconds if there are at least 10 changes in the dataset, and every
     * 60 seconds if there are at least 10000 changes. To completely disable
     * automatic snapshots just set the parameter as an empty string.
     * <li>All the integer parameters representing memory are returned and
     * accepted only using bytes as unit.
     * </ul>
     * 
     * @param parameter
     * @param value
     * @return Status code reply
     */
    public String configSet(final String parameter, final String value) {
	client.configSet(parameter, value);
	return client.getStatusCodeReply();
    }

    public Object eval(String script, int keyCount, String... params) {
	client.setTimeoutInfinite();
	client.eval(script, keyCount, params);

	return getEvalResult();
    }

    public void subscribe(final JedisPubSub jedisPubSub,
	    final String... channels) {
	client.setTimeoutInfinite();
	jedisPubSub.proceed(client, channels);
	client.rollbackTimeout();
    }

    public Long publish(final String channel, final String message) {
	checkIsInMulti();
	connect();
	client.publish(channel, message);
	return client.getIntegerReply();
    }

    public void psubscribe(final JedisPubSub jedisPubSub,
	    final String... patterns) {
	checkIsInMulti();
	connect();
	client.setTimeoutInfinite();
	jedisPubSub.proceedWithPatterns(client, patterns);
	client.rollbackTimeout();
    }

    protected static String[] getParams(List<String> keys, List<String> args) {
	int keyCount = keys.size();
	int argCount = args.size();

	String[] params = new String[keyCount + args.size()];

	for (int i = 0; i < keyCount; i++)
	    params[i] = keys.get(i);

	for (int i = 0; i < argCount; i++)
	    params[keyCount + i] = args.get(i);

	return params;
    }

    public Object eval(String script, List<String> keys, List<String> args) {
	return eval(script, keys.size(), getParams(keys, args));
    }

    public Object eval(String script) {
	return eval(script, 0);
    }

    public Object evalsha(String script) {
	return evalsha(script, 0);
    }

    private Object getEvalResult() {
	return evalResult(client.getOne());
    }

    private Object evalResult(Object result) {
	if (result instanceof byte[])
	    return SafeEncoder.encode((byte[]) result);

	if (result instanceof List<?>) {
	    List<?> list = (List<?>) result;
	    List<Object> listResult = new ArrayList<Object>(list.size());
	    for (Object bin : list) {
		listResult.add(evalResult(bin));
	    }

	    return listResult;
	}

	return result;
    }

    public Object evalsha(String sha1, List<String> keys, List<String> args) {
	return evalsha(sha1, keys.size(), getParams(keys, args));
    }

    public Object evalsha(String sha1, int keyCount, String... params) {
	checkIsInMulti();
	client.evalsha(sha1, keyCount, params);

	return getEvalResult();
    }

    public Boolean scriptExists(String sha1) {
	String[] a = new String[1];
	a[0] = sha1;
	return scriptExists(a).get(0);
    }

    public List<Boolean> scriptExists(String... sha1) {
	client.scriptExists(sha1);
	List<Long> result = client.getIntegerMultiBulkReply();
	List<Boolean> exists = new ArrayList<Boolean>();

	for (Long value : result)
	    exists.add(value == 1);

	return exists;
    }

    public String scriptLoad(String script) {
	client.scriptLoad(script);
	return client.getBulkReply();
    }

    public List<Slowlog> slowlogGet() {
	client.slowlogGet();
	return Slowlog.from(client.getObjectMultiBulkReply());
    }

    public List<Slowlog> slowlogGet(long entries) {
	client.slowlogGet(entries);
	return Slowlog.from(client.getObjectMultiBulkReply());
    }

    public Long objectRefcount(String string) {
	client.objectRefcount(string);
	return client.getIntegerReply();
    }

    public String objectEncoding(String string) {
	client.objectEncoding(string);
	return client.getBulkReply();
    }

    public Long objectIdletime(String string) {
	client.objectIdletime(string);
	return client.getIntegerReply();
    }

    public Long bitcount(final String key) {
	client.bitcount(key);
	return client.getIntegerReply();
    }

    public Long bitcount(final String key, long start, long end) {
	client.bitcount(key, start, end);
	return client.getIntegerReply();
    }

    public Long bitop(BitOP op, final String destKey, String... srcKeys) {
	client.bitop(op, destKey, srcKeys);
	return client.getIntegerReply();
    }

    /**
     * <pre>
     * redis 127.0.0.1:26381> sentinel masters
     * 1)  1) "name"
     *     2) "mymaster"
     *     3) "ip"
     *     4) "127.0.0.1"
     *     5) "port"
     *     6) "6379"
     *     7) "runid"
     *     8) "93d4d4e6e9c06d0eea36e27f31924ac26576081d"
     *     9) "flags"
     *    10) "master"
     *    11) "pending-commands"
     *    12) "0"
     *    13) "last-ok-ping-reply"
     *    14) "423"
     *    15) "last-ping-reply"
     *    16) "423"
     *    17) "info-refresh"
     *    18) "6107"
     *    19) "num-slaves"
     *    20) "1"
     *    21) "num-other-sentinels"
     *    22) "2"
     *    23) "quorum"
     *    24) "2"
     * 
     * </pre>
     * 
     * @return
     */
    @SuppressWarnings("rawtypes")
    public List<Map<String, String>> sentinelMasters() {
	client.sentinel(Protocol.SENTINEL_MASTERS);
	final List<Object> reply = client.getObjectMultiBulkReply();

	final List<Map<String, String>> masters = new ArrayList<Map<String, String>>();
	for (Object obj : reply) {
	    masters.add(BuilderFactory.STRING_MAP.build((List) obj));
	}
	return masters;
    }

    /**
     * <pre>
     * redis 127.0.0.1:26381> sentinel get-master-addr-by-name mymaster
     * 1) "127.0.0.1"
     * 2) "6379"
     * </pre>
     * 
     * @param masterName
     * @return two elements list of strings : host and port.
     */
    public List<String> sentinelGetMasterAddrByName(String masterName) {
	client.sentinel(Protocol.SENTINEL_GET_MASTER_ADDR_BY_NAME, masterName);
	final List<Object> reply = client.getObjectMultiBulkReply();
	return BuilderFactory.STRING_LIST.build(reply);
    }

    /**
     * <pre>
     * redis 127.0.0.1:26381> sentinel reset mymaster
     * (integer) 1
     * </pre>
     * 
     * @param pattern
     * @return
     */
    public Long sentinelReset(String pattern) {
	client.sentinel(Protocol.SENTINEL_RESET, pattern);
	return client.getIntegerReply();
    }

    /**
     * <pre>
     * redis 127.0.0.1:26381> sentinel slaves mymaster
     * 1)  1) "name"
     *     2) "127.0.0.1:6380"
     *     3) "ip"
     *     4) "127.0.0.1"
     *     5) "port"
     *     6) "6380"
     *     7) "runid"
     *     8) "d7f6c0ca7572df9d2f33713df0dbf8c72da7c039"
     *     9) "flags"
     *    10) "slave"
     *    11) "pending-commands"
     *    12) "0"
     *    13) "last-ok-ping-reply"
     *    14) "47"
     *    15) "last-ping-reply"
     *    16) "47"
     *    17) "info-refresh"
     *    18) "657"
     *    19) "master-link-down-time"
     *    20) "0"
     *    21) "master-link-status"
     *    22) "ok"
     *    23) "master-host"
     *    24) "localhost"
     *    25) "master-port"
     *    26) "6379"
     *    27) "slave-priority"
     *    28) "100"
     * </pre>
     * 
     * @param masterName
     * @return
     */
    @SuppressWarnings("rawtypes")
    public List<Map<String, String>> sentinelSlaves(String masterName) {
	client.sentinel(Protocol.SENTINEL_SLAVES, masterName);
	final List<Object> reply = client.getObjectMultiBulkReply();

	final List<Map<String, String>> slaves = new ArrayList<Map<String, String>>();
	for (Object obj : reply) {
	    slaves.add(BuilderFactory.STRING_MAP.build((List) obj));
	}
	return slaves;
    }

    public String sentinelFailover(String masterName) {
	client.sentinel(Protocol.SENTINEL_FAILOVER, masterName);
	return client.getStatusCodeReply();
    }

    public String sentinelMonitor(String masterName, String ip, int port,
	    int quorum) {
	client.sentinel(Protocol.SENTINEL_MONITOR, masterName, ip,
		String.valueOf(port), String.valueOf(quorum));
	return client.getStatusCodeReply();
    }

    public String sentinelRemove(String masterName) {
	client.sentinel(Protocol.SENTINEL_REMOVE, masterName);
	return client.getStatusCodeReply();
    }

    public String sentinelSet(String masterName,
	    Map<String, String> parameterMap) {
	int index = 0;
	int paramsLength = parameterMap.size() * 2 + 2;
	String[] params = new String[paramsLength];

	params[index++] = Protocol.SENTINEL_SET;
	params[index++] = masterName;
	for (Entry<String, String> entry : parameterMap.entrySet()) {
	    params[index++] = entry.getKey();
	    params[index++] = entry.getValue();
	}

	client.sentinel(params);
	return client.getStatusCodeReply();
    }

    public byte[] dump(final String key) {
	checkIsInMulti();
	client.dump(key);
	return client.getBinaryBulkReply();
    }

    public String restore(final String key, final int ttl,
	    final byte[] serializedValue) {
	checkIsInMulti();
	client.restore(key, ttl, serializedValue);
	return client.getStatusCodeReply();
    }

    public Long pexpire(final String key, final long milliseconds) {
	checkIsInMulti();
	client.pexpire(key, milliseconds);
	return client.getIntegerReply();
    }

    public Long pexpireAt(final String key, final long millisecondsTimestamp) {
	checkIsInMulti();
	client.pexpireAt(key, millisecondsTimestamp);
	return client.getIntegerReply();
    }

    public Long pttl(final String key) {
	checkIsInMulti();
	client.pttl(key);
	return client.getIntegerReply();
    }

    public String psetex(final String key, final int milliseconds,
	    final String value) {
	checkIsInMulti();
	client.psetex(key, milliseconds, value);
	return client.getStatusCodeReply();
    }

    public String set(final String key, final String value, final String nxxx) {
	checkIsInMulti();
	client.set(key, value, nxxx);
	return client.getStatusCodeReply();
    }

    public String set(final String key, final String value, final String nxxx,
	    final String expx, final int time) {
	checkIsInMulti();
	client.set(key, value, nxxx, expx, time);
	return client.getStatusCodeReply();
    }

    public String clientKill(final String client) {
	checkIsInMulti();
	this.client.clientKill(client);
	return this.client.getStatusCodeReply();
    }

    public String clientSetname(final String name) {
	checkIsInMulti();
	client.clientSetname(name);
	return client.getStatusCodeReply();
    }

    public String migrate(final String host, final int port, final String key,
	    final int destinationDb, final int timeout) {
	checkIsInMulti();
	client.migrate(host, port, key, destinationDb, timeout);
	return client.getStatusCodeReply();
    }

    public ScanResult<String> scan(final String cursor) {
	return scan(cursor, new ScanParams());
    }

    public ScanResult<String> scan(final String cursor, final ScanParams params) {
	checkIsInMulti();
	client.scan(cursor, params);
	List<Object> result = client.getObjectMultiBulkReply();
	String newcursor = new String((byte[]) result.get(0));
	List<String> results = new ArrayList<String>();
	List<byte[]> rawResults = (List<byte[]>) result.get(1);
	for (byte[] bs : rawResults) {
	    results.add(SafeEncoder.encode(bs));
	}
	return new ScanResult<String>(newcursor, results);
    }

    public ScanResult<Map.Entry<String, String>> hscan(final String key,
	    final String cursor) {
	return hscan(key, cursor, new ScanParams());
    }

    public ScanResult<Map.Entry<String, String>> hscan(final String key,
	    final String cursor, final ScanParams params) {
	checkIsInMulti();
	client.hscan(key, cursor, params);
	List<Object> result = client.getObjectMultiBulkReply();
	String newcursor = new String((byte[]) result.get(0));
	List<Map.Entry<String, String>> results = new ArrayList<Map.Entry<String, String>>();
	List<byte[]> rawResults = (List<byte[]>) result.get(1);
	Iterator<byte[]> iterator = rawResults.iterator();
	while (iterator.hasNext()) {
	    results.add(new AbstractMap.SimpleEntry<String, String>(SafeEncoder
		    .encode(iterator.next()), SafeEncoder.encode(iterator
		    .next())));
	}
	return new ScanResult<Map.Entry<String, String>>(newcursor, results);
    }

    public ScanResult<String> sscan(final String key, final String cursor) {
	return sscan(key, cursor, new ScanParams());
    }

    public ScanResult<String> sscan(final String key, final String cursor,
	    final ScanParams params) {
	checkIsInMulti();
	client.sscan(key, cursor, params);
	List<Object> result = client.getObjectMultiBulkReply();
	String newcursor = new String((byte[]) result.get(0));
	List<String> results = new ArrayList<String>();
	List<byte[]> rawResults = (List<byte[]>) result.get(1);
	for (byte[] bs : rawResults) {
	    results.add(SafeEncoder.encode(bs));
	}
	return new ScanResult<String>(newcursor, results);
    }

    public ScanResult<Tuple> zscan(final String key, final String cursor) {
	return zscan(key, cursor, new ScanParams());
    }

    public ScanResult<Tuple> zscan(final String key, final String cursor,
	    final ScanParams params) {
	checkIsInMulti();
	client.zscan(key, cursor, params);
	List<Object> result = client.getObjectMultiBulkReply();
	String newcursor = new String((byte[]) result.get(0));
	List<Tuple> results = new ArrayList<Tuple>();
	List<byte[]> rawResults = (List<byte[]>) result.get(1);
	Iterator<byte[]> iterator = rawResults.iterator();
	while (iterator.hasNext()) {
	    results.add(new Tuple(SafeEncoder.encode(iterator.next()), Double
		    .valueOf(SafeEncoder.encode(iterator.next()))));
	}
	return new ScanResult<Tuple>(newcursor, results);
    }

    public String clusterNodes() {
	checkIsInMulti();
	client.clusterNodes();
	return client.getBulkReply();
    }

    public String clusterMeet(final String ip, final int port) {
	checkIsInMulti();
	client.clusterMeet(ip, port);
	return client.getStatusCodeReply();
    }

    public String clusterReset(final Reset resetType) {
	checkIsInMulti();
	client.clusterReset(resetType);
	return client.getStatusCodeReply();
    }

    public String clusterAddSlots(final int... slots) {
	checkIsInMulti();
	client.clusterAddSlots(slots);
	return client.getStatusCodeReply();
    }

    public String clusterDelSlots(final int... slots) {
	checkIsInMulti();
	client.clusterDelSlots(slots);
	return client.getStatusCodeReply();
    }

    public String clusterInfo() {
	checkIsInMulti();
	client.clusterInfo();
	return client.getStatusCodeReply();
    }

    public List<String> clusterGetKeysInSlot(final int slot, final int count) {
	checkIsInMulti();
	client.clusterGetKeysInSlot(slot, count);
	return client.getMultiBulkReply();
    }

    public String clusterSetSlotNode(final int slot, final String nodeId) {
	checkIsInMulti();
	client.clusterSetSlotNode(slot, nodeId);
	return client.getStatusCodeReply();
    }

    public String clusterSetSlotMigrating(final int slot, final String nodeId) {
	checkIsInMulti();
	client.clusterSetSlotMigrating(slot, nodeId);
	return client.getStatusCodeReply();
    }

    public String clusterSetSlotImporting(final int slot, final String nodeId) {
	checkIsInMulti();
	client.clusterSetSlotImporting(slot, nodeId);
	return client.getStatusCodeReply();
    }

    public String clusterSetSlotStable(final int slot) {
	checkIsInMulti();
	client.clusterSetSlotStable(slot);
	return client.getStatusCodeReply();
    }

    public String clusterForget(final String nodeId) {
	checkIsInMulti();
	client.clusterForget(nodeId);
	return client.getStatusCodeReply();
    }

    public String clusterFlushSlots() {
	checkIsInMulti();
	client.clusterFlushSlots();
	return client.getStatusCodeReply();
    }

    public Long clusterKeySlot(final String key) {
	checkIsInMulti();
	client.clusterKeySlot(key);
	return client.getIntegerReply();
    }

    public Long clusterCountKeysInSlot(final int slot) {
	checkIsInMulti();
	client.clusterCountKeysInSlot(slot);
	return client.getIntegerReply();
    }

    public String clusterSaveConfig() {
	checkIsInMulti();
	client.clusterSaveConfig();
	return client.getStatusCodeReply();
    }

    public String clusterReplicate(final String nodeId) {
	checkIsInMulti();
	client.clusterReplicate(nodeId);
	return client.getStatusCodeReply();
    }

    public List<String> clusterSlaves(final String nodeId) {
	checkIsInMulti();
	client.clusterSlaves(nodeId);
	return client.getMultiBulkReply();
    }

    public String clusterFailover() {
	checkIsInMulti();
	client.clusterFailover();
	return client.getStatusCodeReply();
    }

    @Override
    public List<Object> clusterSlots() {
	checkIsInMulti();
	client.clusterSlots();
	return client.getObjectMultiBulkReply();
    }

    public String asking() {
	checkIsInMulti();
	client.asking();
	return client.getStatusCodeReply();
    }

    public List<String> pubsubChannels(String pattern) {
	checkIsInMulti();
	client.pubsubChannels(pattern);
	return client.getMultiBulkReply();
    }

    public Long pubsubNumPat() {
	checkIsInMulti();
	client.pubsubNumPat();
	return client.getIntegerReply();
    }

    public Map<String, String> pubsubNumSub(String... channels) {
	checkIsInMulti();
	client.pubsubNumSub(channels);
	return BuilderFactory.PUBSUB_NUMSUB_MAP.build(client
		.getBinaryMultiBulkReply());
    }

    @Override
    public void close() {
	if (dataSource != null) {
	    if (client.isBroken()) {
		this.dataSource.returnBrokenResource(this);
	    } else {
		this.dataSource.returnResource(this);
	    }
	} else {
	    client.close();
	}
    }

    public void setDataSource(Pool<Jedis> jedisPool) {
	this.dataSource = jedisPool;
    }

    public Long pfadd(final String key, final String... elements) {
	checkIsInMulti();
	client.pfadd(key, elements);
	return client.getIntegerReply();
    }

    public long pfcount(final String key) {
	checkIsInMulti();
	client.pfcount(key);
	return client.getIntegerReply();
    }

    @Override
    public long pfcount(String... keys) {
	checkIsInMulti();
	client.pfcount(keys);
	return client.getIntegerReply();
    }

    public String pfmerge(final String destkey, final String... sourcekeys) {
	checkIsInMulti();
	client.pfmerge(destkey, sourcekeys);
	return client.getStatusCodeReply();
    }

    @Override
    public List<String> blpop(int timeout, String key) {
	checkIsInMulti();
	List<String> args = new ArrayList<String>();
	args.add(key);
	args.add(String.valueOf(timeout));
	client.blpop(args.toArray(new String[args.size()]));
	client.setTimeoutInfinite();
	final List<String> multiBulkReply = client.getMultiBulkReply();
	client.rollbackTimeout();
	return multiBulkReply;
    }

    @Override
    public List<String> brpop(int timeout, String key) {
	checkIsInMulti();
	List<String> args = new ArrayList<String>();
	args.add(key);
	args.add(String.valueOf(timeout));
	client.brpop(args.toArray(new String[args.size()]));
	client.setTimeoutInfinite();
	final List<String> multiBulkReply = client.getMultiBulkReply();
	client.rollbackTimeout();
	return multiBulkReply;
    }

}
