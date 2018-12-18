package com.netty.server;

import java.util.Set;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import com.netty.constant.Constant;
import com.netty.init.AfterSpringBegin;
import com.netty.util.RedisDB;
import com.netty.util.SerializeUtil;
import com.netty.util.UpdateHistoryMsgToRedis;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import redis.clients.jedis.Jedis;

/**
 * 启动服务
 * */

public class WebSocketServer extends AfterSpringBegin{

	//用于客户端连接请求
	@Autowired
	private EventLoopGroup bossGroup;
	
	//用于处理客户端I/O操作
	@Autowired
	private EventLoopGroup workerGroup;
	
	//服务器的辅助启动类
	@Autowired
	private ServerBootstrap serverBootstrap;
	
	//BS的I/O处理类
	private ChannelHandler childChannelHandler;
	
	private ChannelFuture channelFuture;
	
	//服务端口
	private int port;
	
	public WebSocketServer(){
		
		System.out.println("初始化");
	}

	public EventLoopGroup getBossGroup() {
		return bossGroup;
	}

	public void setBossGroup(EventLoopGroup bossGroup) {
		this.bossGroup = bossGroup;
	}

	public EventLoopGroup getWorkerGroup() {
		return workerGroup;
	}

	public void setWorkerGroup(EventLoopGroup workerGroup) {
		this.workerGroup = workerGroup;
	}

	public ServerBootstrap getServerBootstrap() {
		return serverBootstrap;
	}

	public void setServerBootstrap(ServerBootstrap serverBootstrap) {
		this.serverBootstrap = serverBootstrap;
	}

	public ChannelHandler getChildChannelHandler() {
		return childChannelHandler;
	}

	public void setChildChannelHandler(ChannelHandler childChannelHandler) {
		this.childChannelHandler = childChannelHandler;
	}

	public ChannelFuture getChannelFuture() {
		return channelFuture;
	}

	public void setChannelFuture(ChannelFuture channelFuture) {
		this.channelFuture = channelFuture;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		Jedis jedis = null;
		try {
			jedis = RedisDB.getJedis();
			//从redis中获取联系人列表
			loadAllContactsListFromRedis(jedis);
			//加载所有redis 中的聊天记录
			loadAllHistoryMessageFromRedis(jedis);
			//加载所有redis中的未读聊天记录数
			loadAllHistoryMessageCountFromRedis(jedis);
			jedis.close();
			//启动线程将聊天信息存入redis
			new Thread(new UpdateHistoryMsgToRedis()).start();
			
			bulid(port);
			
		} catch (Exception e) {
			e.printStackTrace();
            RedisDB.returnBrokenResource(jedis);
		}finally {
			RedisDB.returnResource(jedis);
		}
	}
	
	public void bulid(int port) throws Exception{
		
		try {
			
			//（1）boss辅助客户端的tcp连接请求  worker负责与客户端之前的读写操作
			//（2）配置客户端的channel类型
			//(3)配置TCP参数，握手字符串长度设置
			//(4)TCP_NODELAY是一种算法，为了充分利用带宽，尽可能发送大块数据，减少充斥的小块数据，true是关闭，可以保持高实时性,若开启，减少交互次数，但是时效性相对无法保证
			//(5)开启心跳包活机制，就是客户端、服务端建立连接处于ESTABLISHED状态，超过2小时没有交流，机制会被启动
			//(6)netty提供了2种接受缓存区分配器，FixedRecvByteBufAllocator是固定长度，但是拓展，AdaptiveRecvByteBufAllocator动态长度
			//(7)绑定I/O事件的处理类,WebSocketChildChannelHandler中定义
			serverBootstrap.group(bossGroup,workerGroup)
						   .channel(NioServerSocketChannel.class)
						   .option(ChannelOption.SO_BACKLOG, 1024)
						   .option(ChannelOption.TCP_NODELAY, true)
						   .childOption(ChannelOption.SO_KEEPALIVE, true)
						   .childOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(592048))
						   .childHandler(childChannelHandler);
			
			System.out.println("成功");
			channelFuture = serverBootstrap.bind(port).sync();
			channelFuture.channel().closeFuture().sync();
		} catch (Exception e) {
			// TODO: handle exception
			bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
		}
	}
	
	//加载所有redis 中的聊天记录
	public void loadAllHistoryMessageFromRedis(Jedis jedis) {
		jedis.select(RedisDB.dbSelectedForHistoryMessage);
		//获得所有的key
		Set<String> keys = jedis.keys("history_*");
		for(String key : keys) {
			byte[] msg = jedis.get(key.getBytes());
			Constant.allHistoryMessage.put(key, SerializeUtil.unserializeForList(msg));
		}
	}
	
	//加载所有redis中的未读聊天记录数
	public void loadAllHistoryMessageCountFromRedis(Jedis jedis) {
		jedis.select(RedisDB.dbSelectedForHistoryMessage);
		//获得所有的key
		Set<String> keys = jedis.keys("unread_*");
		for(String key : keys) {
			String msg = jedis.get(key);
			Constant.unreadHistoryMessage.put(key, Integer.valueOf(msg));
		}
	}
	
	//从redis中获取联系人列表
	public void loadAllContactsListFromRedis(Jedis jedis) {
		jedis.select(RedisDB.dbSelectedForSystem);
		Constant.contactsList = SerializeUtil.unserializeForList(jedis.get(RedisDB.systemUsers.getBytes()));
	}
	
	
	//执行之后关闭
	@PreDestroy
	public void close(){
		bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
	}
}
