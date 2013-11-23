package de.voidplus.redis;

import java.net.URI;

import processing.core.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;

public class Redis extends Jedis {
	
	private PApplet parent;
	public final static String VERSION = "1.0";
	
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
		this.parent.println("# Redis-Wrapper v"+this.version()+" - https://github.com/voidplus/redis-processing & https://github.com/xetorthio/jedis");
		this.parent.registerDispose(this);
	}
    
	public static String version() {
		return VERSION;
	}
	
	public void dispose(){
		if(this.isConnected()){
			this.disconnect();
		}
	}
	
}