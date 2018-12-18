package com.netty.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.netty.constant.Constant;
import com.netty.entity.OneToOneMessage;
import redis.clients.jedis.Jedis;

public class UpdateHistoryMsgToRedis implements Runnable{

	static SimpleDateFormat ymdhms = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
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
				
				//修改当前登陆的用户 状态为在线 然后在存入redis   注：放入排序后的联系人列表
				jedis.select(RedisDB.dbSelectedForSystem);
				synchronized (Constant.contactsList) {
					
					//再次从redis获取联系人列表
					List<Map<String,Object>> redis_contactsList = SerializeUtil.unserializeForList(jedis.get(RedisDB.systemUsers.getBytes()));
					
					Constant.contactsList .clear();
					//需要修改状态的联系人
					List<Map<String,Object>>  contactsList_offLine = new ArrayList<Map<String,Object>>();
					
					for (Map<String, Object> map : redis_contactsList) {
						//pushCtxMap 里面是在线人列表    如果联系人没有在pushCtxMap里面被维护   说明这个人在webSocket中是离线状态  要更新进redis
						if(!Constant.pushCtxMap.containsKey(map.get("id").toString())) {  
							if(Boolean.valueOf(map.get("isOnline").toString())) {  //webSocket中是离线状态    redis是在线状态 
								map.put("isOnline", false);
								contactsList_offLine.add(map);
								continue;
							}
						}
						Constant.contactsList .add(map);
					}
					if(contactsList_offLine.size() != 0) {
						Constant.contactsList .addAll(contactsList_offLine);
					}
					jedis.set(RedisDB.systemUsers.getBytes(), SerializeUtil.serialize(Constant.contactsList));
					
					for (Map<String, Object> ttt : Constant.contactsList) {
						System.out.println("["+ymdhms.format(new Date())+"]name:"+ttt.get("name")+"-----nickName:"+ttt.get("nickName")+"-----isOnline:"+ttt.get("isOnline"));
					}
					
				}
			} catch (Exception e) {
				e.printStackTrace();
				RedisDB.returnBrokenResource(jedis);
			}finally {
				RedisDB.returnResource(jedis);
				try {
					Thread.sleep(30000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
