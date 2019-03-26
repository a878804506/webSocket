package com.netty.constant;

import com.alibaba.fastjson.JSON;
import com.netty.util.Base64AndPictureUtil;
import com.netty.util.FtpUtil;
import io.netty.channel.ChannelHandlerContext;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.netty.entity.OneToOneMessage;

import javax.imageio.ImageIO;

import static com.netty.util.FtpUtil.messageFilePath;

/**
 * 常量池
 * */
public class Constant {
	//存放所有的ChannelHandlerContext  
	//key : userId
	public static Map<String, ChannelHandlerContext> pushCtxMap = new ConcurrentHashMap<String, ChannelHandlerContext>() ;
	
	//存放某一类的channel
	public static ChannelGroup aaChannelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	
	//redis中所有的聊天记录  服务启动加载redis中的聊天记录
	public static Map<String,List<OneToOneMessage>> allHistoryMessage = new HashMap<String, List<OneToOneMessage>>();
	
	//未读的聊天消息   key : from_To_to_unread    value : 未读消息条数
	public static Map<String,Integer> unreadHistoryMessage = new HashMap<String, Integer>();
	
	//联系人列表
	public static List<Map<String,Object>> contactsList = new LinkedList<Map<String, Object>>();
	
	public static  SimpleDateFormat ymdhms = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	//生成 一对一聊天的 redis key
	public static String getOneToOneMessageKey(Integer fromUserId,Integer toUserId) {
		String results = "history_";
		if(fromUserId.compareTo(toUserId) > 0) {      //str1的字典序大于str2的字典序，则交换两者变量
			results += fromUserId+"_to_"+toUserId;
        }else {
        	results += toUserId+"_to_"+fromUserId;
        }
		return results;
	}
	
	//生成 一对一 未读聊天的  key    from_To_to_unread
	public static String getOneToOneUnReadMessageKey(Integer fromUserId,Integer toUserId) {
		return "unread_"+fromUserId+"_to_"+toUserId;
	}
	
	/**
	 * 返回带有未读条数 的最新联系人列表
	 * @param contactsList  最新联系人列表
	 * @param toUserId  当前登陆用户
	 * @return
	 */
	public static List<Map<String,Object>> getOneToOneUnReadMessageCount(List<Map<String,Object>> contactsList ,Integer toUserId) {
		for (Map<String, Object> temp : contactsList) {
			String OneToOneUnReadMessageCountKey = getOneToOneUnReadMessageKey(Integer.valueOf(temp.get("id").toString()),toUserId);
			if(unreadHistoryMessage.containsKey(OneToOneUnReadMessageCountKey)) {
				temp.put("unread", unreadHistoryMessage.get(OneToOneUnReadMessageCountKey));
			}else {
				temp.put("unread", 0);
			}
		}
		return contactsList;
	}
	
	//加入聊天历史集合
	public static void addAllHistoryMessage(OneToOneMessage oneToOneMessage) {
		// 获取 key
 		String oneToOneMessageKey = Constant.getOneToOneMessageKey(oneToOneMessage.getFrom(),oneToOneMessage.getTo());
 		//聊天记录
		List<OneToOneMessage> list = new LinkedList<>();
		if(Constant.allHistoryMessage.containsKey(oneToOneMessageKey)) {
			list = (List<OneToOneMessage>) Constant.allHistoryMessage.get(oneToOneMessageKey);
		}
 		//加入最新聊天记录 并再次存入历史聊天集合中
 		list.add(oneToOneMessage);
 		synchronized (Constant.allHistoryMessage) {  //存入时加锁
 			Constant.allHistoryMessage.put(oneToOneMessageKey, list);
		}
	}
	
	//加入未读集合
	public static void addunreadHistoryMessage(OneToOneMessage oneToOneMessage) {
		// 获取 key
 		String oneToOneMessageCountKey = Constant.getOneToOneUnReadMessageKey(oneToOneMessage.getFrom(),oneToOneMessage.getTo());
 		// 获取未读聊天记录条数  然后加1
 		int unReadMsgCount = 0;
		if(Constant.unreadHistoryMessage.containsKey(oneToOneMessageCountKey)) {
			unReadMsgCount =  Constant.unreadHistoryMessage.get(oneToOneMessageCountKey) +1;
		}else {
			unReadMsgCount = 1;
		}
 		synchronized (Constant.unreadHistoryMessage) {  //存入时加锁
 			Constant.unreadHistoryMessage.put(oneToOneMessageCountKey, unReadMsgCount);
		}
	}

	//将用户发送的图片上传到ftp服务器
	public static String uploadPicToFTP(OneToOneMessage picMsg){
		try{
			//  picMsg.getId()  为 生成图片id
			String temp = picMsg.getData().split(";")[0].split("/")[1];
			if("jpeg".equals(temp) || "gif".equals(temp) || "png".equals(temp) || "bmp".equals(temp)){
				picMsg.setId(picMsg.getId()+"."+temp);
			}else{
				// 非图片

			}
			//本服务器的图片路径
			String localFilePath = FtpUtil.localFilePath + System.getProperty("file.separator")+ picMsg.getId();
			// base64 转换成图片
			Base64AndPictureUtil.Base64ToImage(picMsg.getData(), localFilePath);
			//输出目录+输出文件
			File out = new File(localFilePath);
			//上传图片到ftp服务器
			List<String> fileNames = new ArrayList<>();
			fileNames.add(picMsg.getId());
			//上传
			FtpUtil.uploadLocalFile(FtpUtil.messageFilePath+"/",fileNames);
			//删除临时图片
			out.delete();
			return picMsg.getId();
		}catch (Exception e){
			return "false";
		}finally {

		}
	}
}
