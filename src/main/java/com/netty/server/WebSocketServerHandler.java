package com.netty.server;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.netty.constant.Constant;
import com.netty.entity.OneToOneMessage;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

/**
 * websocket 具体业务处理方法
 * 
 */

@Component
@Sharable
public class WebSocketServerHandler extends BaseWebSocketServerHandler {

	private WebSocketServerHandshaker handshaker;
	
	/**
	 * 当客户端连接成功，返回个成功信息
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
//		ctx.channel().write(new TextWebSocketFrame("server:主动给客户端发消息"));
		ctx.flush();
	}

	/**
	 * 当客户端断开连接
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		//剔除ChannelHandlerContext
		for (String key : Constant.pushCtxMap.keySet()) {
			if (ctx.equals(Constant.pushCtxMap.get(key))) {
				// 从连接池内剔除
				System.out.println(Constant.pushCtxMap.size()+"剔除" + key);
				Constant.pushCtxMap.remove(key);
				System.out.println(",之后的个数为"+Constant.pushCtxMap.size());
			}
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		// http：//xxxx
		if (msg instanceof FullHttpRequest) {
			handleHttpRequest(ctx, (FullHttpRequest) msg);
		} else if (msg instanceof WebSocketFrame) {
			// ws://xxxx
			handlerWebSocketFrame(ctx, (WebSocketFrame) msg);
		}
	}

	public void handlerWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
		// 关闭请求
		if (frame instanceof CloseWebSocketFrame) {
			handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
			return;
		}
		// ping请求
		if (frame instanceof PingWebSocketFrame) {
			ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
			return;
		}
		// 只支持文本格式，不支持二进制消息
		if (!(frame instanceof TextWebSocketFrame)) {
			throw new Exception("仅支持文本格式");
		}
		/**
         * 本例程仅支持文本消息，不支持二进制消息
         */
        if (frame instanceof BinaryWebSocketFrame) {
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
        }
        
        // 客服端发送过来的消息
 		String request = ((TextWebSocketFrame) frame).text();
 		System.out.println("服务端收到：" + request);
 		JSONObject jsonObject = null;
 		try {
 			jsonObject = JSONObject.parseObject(request);
 			System.out.println(jsonObject.toJSONString());
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 		if (jsonObject == null) {
 			return;
 		}
 		
        if(frame instanceof TextWebSocketFrame){
            // 返回应答消息
        	Map<String,Object> systemMsg = new HashMap<>();
			systemMsg.put("id", "system");
			systemMsg.put("type", -1);
			systemMsg.put("data", "服务器收到并返回了你发送的JSON："+request);
			ctx.channel().write(new TextWebSocketFrame(JSON.toJSONString(systemMsg)));
        }
        // 消息类型
        String type = jsonObject.get("type").toString();
        if("2".equals(type)) {  //JSON定义type=2 ----> 一对一聊天
            String msgType = jsonObject.get("msgType").toString();
            OneToOneMessage oneToOneMessage = new OneToOneMessage();
            oneToOneMessage.setId(jsonObject.get("id").toString());
            oneToOneMessage.setMsgType(msgType);
            oneToOneMessage.setFrom(Integer.valueOf(jsonObject.get("from").toString()));
            oneToOneMessage.setTo(Integer.valueOf(jsonObject.get("to").toString()));
            oneToOneMessage.setData(jsonObject.get("data").toString());
            oneToOneMessage.setDate(Constant.ymdhms.format(new Date()));
            switch (msgType){
                case "0":  //文本消息
                case "1":  //表情消息
                    if(Constant.pushCtxMap.containsKey(oneToOneMessage.getTo().toString())) {//找到目标用户
                        push(Constant.pushCtxMap.get(oneToOneMessage.getTo().toString()),JSON.toJSONString(oneToOneMessage));
                    }else {//不在线
                        System.out.println("消息发送的目标用户不在线！");
                    }
                    //加入未读集合
                    Constant.addunreadHistoryMessage(oneToOneMessage);
                    //加入聊天历史集合
                    Constant.addAllHistoryMessage(oneToOneMessage);
                    break;
                case "2": //图片消息
					String isSuccess = Constant.uploadPicToFTP(oneToOneMessage); //此处用字符串返回 如果为“false” 则上传失败，如果为文件名字，则成功
					System.out.println(isSuccess);
					if("false".equals(isSuccess)){
						// 发送失败就告知发送者 发送失败

					}else{
						//  发送成功就告知发送者发送成功，并且将图片消息发送到目标用户

					}
                    break;
            }
        }else if("3".equals(type)) { //客户端要求拉取一对一聊天记录
        	List<OneToOneMessage> list = new LinkedList<>();
        	if("0".equals(jsonObject.get("msgDate").toString())) {  //只拉取最近三天的一对一聊天记录
         		// 获取 key
         		String oneToOneMessageKey = Constant.getOneToOneMessageKey(Integer.valueOf(jsonObject.get("from").toString()),Integer.valueOf(jsonObject.get("to").toString()));
         		//聊天记录
         		if(Constant.allHistoryMessage.containsKey(oneToOneMessageKey)) {
					list = Constant.allHistoryMessage.get(oneToOneMessageKey);
				}
        	}else { //全部记录
        		// 巴拉巴拉。。。。。。。。。。。
        		
        	}
        	//置为0条未读消息
        	Constant.unreadHistoryMessage.put(Constant.getOneToOneUnReadMessageKey(Integer.valueOf(jsonObject.get("to").toString()),Integer.valueOf(jsonObject.get("from").toString())),0);

        	Map<String,Object> oneToOneHistoryMessage = new HashMap<>();
     		oneToOneHistoryMessage.put("id", "");
     		oneToOneHistoryMessage.put("type", 3);
     		oneToOneHistoryMessage.put("data", JSON.toJSONString(list));
     		ctx.channel().write(new TextWebSocketFrame(JSON.toJSONString(oneToOneHistoryMessage)));
        }else if("4".equals(type)) { // 客户端告知已读消息
        	//置为0条未读消息
        	Constant.unreadHistoryMessage.put(Constant.getOneToOneUnReadMessageKey(Integer.valueOf(jsonObject.get("from").toString()),Integer.valueOf(jsonObject.get("to").toString())),0);
        }else if("5".equals(type)) {
        	// 用户下线通知，这里没有客户端向服务器发送5请求
        }else if("6".equals(type)) {
			// 这里客户端发送心跳时 会带上自己的userId和sessionId  可以在这里验证一下

			Map<String,Object> pongToClient = new HashMap<>();
			pongToClient.put("id", "pongTo6");
			pongToClient.put("type", 6);
			pongToClient.put("stauts", true);
			ctx.channel().write(new TextWebSocketFrame(JSON.toJSONString(pongToClient)));
		}
	}

	// 第一次请求是http请求，请求头包括ws的信息
	public void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req)  {
		if (!req.decoderResult().isSuccess()) {
			sendHttpResponse(ctx, req,new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
			return;
		}
		//获取http请求的参数
		QueryStringDecoder decoder = new QueryStringDecoder(req.uri());
        Map<String, List<String>> paramList = decoder.parameters();
        String msg = "";
        for (Map.Entry<String, List<String>> entry : paramList.entrySet()) {
            System.out.println(entry.getKey()+"----------------"+entry.getValue().get(0));
            msg = entry.getValue().get(0);
        }
        
        JSONObject jsonObject = null;
		try {
			jsonObject = JSONObject.parseObject(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (jsonObject == null) {
			return;
		}
		
		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory
                ("ws:/" + ctx.channel() + "/websocket", null, false,65535*10);
		handshaker = wsFactory.newHandshaker(req);
		if (handshaker == null) {
			// 不支持
			WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
		} else {
			
			//websocket连接校验  开始
			String sessionId = (String) jsonObject.get("id");
			String userId = (String)jsonObject.get("userId").toString();

			//先不写

			System.out.println("当客户端连接成功，返回个成功信息");
			push(ctx, "服务器收到并返回：连接成功！@！@！@！@");
			
			//websocket连接校验  结束
			
			//加入列表
	        Constant.pushCtxMap.put(userId, ctx);
	        Constant.aaChannelGroup.add(ctx.channel());
	        
			handshaker.handshake(ctx.channel(), req);
			Map<String,Object> systemMsg = new HashMap<>();
			systemMsg.put("id", "system");
			systemMsg.put("type", -1);
			systemMsg.put("data", "服务器推送消息：登陆成功！！！@@@");
			ctx.channel().write(new TextWebSocketFrame(JSON.toJSONString(systemMsg)));
	        
	        //当前登陆用户的联系人列表
			Map<String,Object> ContactsMap = new HashMap<>();
			ContactsMap.put("id", userId);
			ContactsMap.put("type", 0);
			ContactsMap.put("data", Constant.getOneToOneUnReadMessageCount(Constant.contactsList, Integer.valueOf(userId)));
			ctx.channel().write(new TextWebSocketFrame(JSON.toJSONString(ContactsMap)));
		}
	}

	public static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, DefaultFullHttpResponse res) {
		// 返回应答给客户端
		if (res.status().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
		}
		// 如果是非Keep-Alive，关闭连接
		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if (!isKeepAlive(req) || res.status().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private static boolean isKeepAlive(FullHttpRequest req) {
		return false;
	}

	// 异常处理，netty默认是关闭channel
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		// 输出日志
		cause.printStackTrace();
//		ctx.close();
	}
}
