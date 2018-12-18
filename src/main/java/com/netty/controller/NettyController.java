package com.netty.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.netty.constant.Constant;
import com.netty.server.BaseWebSocketServerHandler;




@Controller
public class NettyController {
	
	@RequestMapping("/test")
	@ResponseBody
	public String topicSend(String message){
		String result = "xxxxxxxxx";
		BaseWebSocketServerHandler.push(Constant.aaChannelGroup, message);
		return result;
	}
	
	@RequestMapping("/go")
	public String go(){
		return "index.jsp";
	}
}
