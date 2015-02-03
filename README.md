# Redis for Processing

Wrapper to use [Redis](http://redis.io/) in [Processing](http://processing.org/). It's based on [Jedis](https://github.com/xetorthio/jedis), a small Java client by [Jonathan Leibiusky](https://github.com/xetorthio).


## Table of Contents

- [About](#about)
- [Download](#download)
- [Installation](#installation)
- [Tested](#tested)
- [Examples](#examples)
- [Usage](#usage)
- [Links](#links)
- [Questions?](#questions)
- [License](#license)


## About

[Introduction to Redis](http://redis.io/topics/introduction): Redis is an open source, BSD licensed, advanced key-value store. It is often referred to as a data structure server since keys can contain strings, hashes, lists, sets and sorted sets.


## Download

* [Redis-Wrapper v1.1.2](download/Redis.zip?raw=true) includes **Jedis v3.0.0** with **Redis v2.8.5** support.


## Installation


### Processing library

Unzip and put the extracted *Redis* folder into the libraries folder of your Processing sketches. Reference and examples are included in the *Redis* folder. For more help read the [tutorial](http://www.learningprocessing.com/tutorials/libraries/) by [Daniel Shiffman](https://github.com/shiffman).


### Redis server

You can follow the official installation steps [here](http://redis.io/download):

```bash
$ wget http://download.redis.io/releases/redis-2.8.19.tar.gz
$ tar xzf redis-2.8.19.tar.gz
$ cd redis-2.8.19
$ make
```

Or you can use [Homebrew](http://brew.sh/) (OS X):

```bash
$ brew install redis
```

## Tested

System:

* **OSX** (10.10 Yosemite)
* **Windows** (*not tested yet, but x86 and x64 should work*)

Processing Version:

* **2.2.1**
* 2.1.1
* 2.1.0


## Examples

* [Basic](#usage) → [e1_basic.pde](examples/e1_basic/e1_basic.pde)


## Usage

Start a Redis server:

```bash
$ redis-server --port 6379
```

Run the following sketch:

```java
import de.voidplus.redis.*;

Redis redis;

void setup(){
    // ...
    
    redis = new Redis(this, "127.0.0.1", 6379);
    
    redis.set("key", "value");
    println(redis.get("key"));
}
```


## Links

Useful links for developers:

* [Jedis](https://github.com/xetorthio/jedis#so-what-can-i-do-with-jedis)
* [Redis](http://redis.io/)
* [Redis Documentation](http://redis.io/documentation)
* [Redis Commands](http://redis.io/commands)


## Questions?

Don't be shy and feel free to contact me on Twitter: [@darius_morawiec](https://twitter.com/darius_morawiec)


## License

The wrapper is Open Source Software released under the [License](LICENSE.txt).