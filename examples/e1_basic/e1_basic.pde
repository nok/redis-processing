import de.voidplus.redis.*;

Redis redis;

void setup(){
    // ...

    redis = new Redis(this, "127.0.0.1", 6379);

    redis.set("key", "value");
    println(redis.get("key"));
}
