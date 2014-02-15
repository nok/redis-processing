package de.voidplus.redis;

import processing.core.*;
import java.net.URI;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;


public class Redis extends Jedis {
	
	private PApplet parent;
	public final static String WRAPPER_VERSION = "1.1";
	public final static String JEDIS_VERSION = "2.4.0";
	public final static String REDIS_VERSION = "2.8.5";
	
	public Redis(PApplet parent, String host) {
		super(host);
		this.init(parent);
	}

	public Redis(PApplet parent, String host, int port) {
		super(host, port);
		this.init(parent);
	}

    public Redis(PApplet parent, final String host, final int port, final int timeout) {
    	super(host, port, timeout);
    	this.init(parent);
    }

    public Redis(PApplet parent, JedisShardInfo shardInfo) {
    	super(shardInfo);
    	this.init(parent);
    }

    public Redis(PApplet parent, URI uri) {
    	super(uri);
    	this.init(parent);
    }
	
	private void init(PApplet parent){
		this.parent = parent;
		this.parent.println("# Redis-Wrapper v"+Redis.WRAPPER_VERSION+" - https://github.com/voidplus/redis-processing");
		this.parent.println("# Jedis-Library v"+Redis.JEDIS_VERSION+" with Redis v"+Redis.REDIS_VERSION+" support - https://github.com/xetorthio/jedis");
		this.parent.registerDispose(this);
		
		try {
			this.ping();
		} catch (Exception e){
			this.parent.println("# Error: Redis-Serer not found ... exit sketch.");
			this.parent.println(e.getMessage());
			this.parent.exit();
		}
	}
	
	public void dispose(){
		if(this.isConnected()){
			this.disconnect();
		}
	}
	
}