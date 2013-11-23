# Redis for Processing

Wrapper to use [Redis](http://redis.io/) in [Processing](http://processing.org/). It's based on [Jedis](https://github.com/xetorthio/jedis), a small Java client by [Jonathan Leibiusky](https://github.com/xetorthio).


## About

> [Introduction to Redis](http://redis.io/topics/introduction): Redis is an open source, BSD licensed, advanced key-value store. It is often referred to as a data structure server since keys can contain strings, hashes, lists, sets and sorted sets.

## Download

* [Redis v1.0](https://raw.github.com/voidplus/redis-processing/master/download/Redis.zip) (Redis v2.6.14)


## Installation

#### Processing library:

Unzip and put the extracted *Redis* folder into the libraries folder of your Processing sketches. Reference and examples are included in the *Redis* folder.

#### Redis server:

You can follow the official installation steps [here](http://redis.io/download):

```
$ wget http://download.redis.io/releases/redis-2.8.0.tar.gz
$ tar xzf redis-2.8.0.tar.gz
$ cd redis-2.8.0
$ make
```

Or use [Homebrew](http://brew.sh/) (OSX):

```
$ brew install redis
```



## Usage

Start a Redis server:

```
$ redis-server --port 6379
```

Run the following sketch:

```
import de.voidplus.redis.*;

Redis redis;

void setup(){
    // ...
    
    redis = new Redis(this, "127.0.0.1", 6379);
    
    redis.set("key", "value");
    println(redis.get("key"));
}
```


## Examples

* [Basic](https://github.com/voidplus/redis-processing/blob/master/examples/e1_basic/e1_basic.pde)


## Tested

System:

* OSX
* Windows (*not tested yet, but x86 and x64 should work*)

Processing Version:

* 2.1.0


## Links

Useful links for developers:

* [Jedis](https://github.com/xetorthio/jedis#so-what-can-i-do-with-jedis)
* [Redis](http://redis.io/)
* [Redis Documentation](http://redis.io/documentation)
* [Redis Commands](http://redis.io/commands)


## Questions?

Feel free to contact me via [Twitter](http://twitter.voidplus.de).


## License

The wrapper is Open Source Software released under the [License](https://raw.github.com/voidplus/redis-processing/master/LICENSE.txt). It's developed by [Darius Morawiec](http://voidplus.de).