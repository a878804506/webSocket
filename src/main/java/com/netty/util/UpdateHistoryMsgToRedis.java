package com.netty.util;

import java.util.List;
import java.util.Map.Entry;

import com.netty.constant.Constant;
import com.netty.entity.OneToOneMessage;
import redis.clients.jedis.Jedis;

public class UpdateHistoryMsgToRedis implements Runnable{

	@Override
	public void run() {
		while(true) {
			Jedis jedis = null;
			try {
				jedis = RedisDB.getJedis();
				jedis.select(RedisDB.dbSelectedForHistoryMessage);
				// 聊天记录定期存入reids
				synchronized (Constant.allHistoryMessage) {
					if(Constant.allHistoryMessage.size() != 0) {
						for (Entry<String, List<OneToOneMessage>> entry : Constant.allHistoryMessage.entrySet()) {
							jedis.set(entry.getKey().getBytes(), SerializeUtil.serialize(entry.getValue()));
						}
					}
				}
				
				// 一对一未读消息 定期存入redis
				synchronized (Constant.unreadHistoryMessage) {
					if(Constant.unreadHistoryMessage.size() != 0) {
						for (Entry<String, Integer> entry : Constant.unreadHistoryMessage.entrySet()) {
							if( 0 == entry.getValue()) { //0条未读就	清空redis
								jedis.del(entry.getKey());
							}else {
								jedis.set(entry.getKey(), entry.getValue().toString());
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				RedisDB.returnBrokenResource(jedis);
			}finally {
				RedisDB.returnResource(jedis);
				try {
					Thread.sleep(60*60*1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
