<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<%
	String path = request.getContextPath();
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">  
<html xmlns="http://www.w3.org/1999/xhtml">  
<head>  
<title>Netty WebSocket DEMO</title>

<link rel="stylesheet" type="text/css" href="<%=path%>/font_Icon/iconfont.css">
<link rel="stylesheet" type="text/css" href="<%=path %>/css/chat.css">

<style type="text/css">
	#contactsName{
		font-size:10px;
	}
</style>
<script src="<%=path %>/js/jquery.min.js"></script>
<script type="text/javascript">
    	var socket;
    	var sessionId = "314";
		var userId = "10005";
		var userName = "会飞的鱼🐟";
		var me ; //自己
		var contactsList; //最新联系人列表
		var contactsListForPicture = new Map(); // key : userId    value : img/grayImg
		/* var sessionId = "147258";
		var userId = "2";
		var userName = "天使👼"; */
		
		//联系人上线 文字颜色切换效果
		/* 红：255，0，0    #FF0000
		橙:  255,125,0     #FF7D00
		黄：255，255，0   #FFFF00
		绿：0，255，0    #00FF00
		蓝：0，0，255    #0000FF
		靛: 0,255,255    #00FFFF
		紫: 255,0,255    #FF00FF */
		var colors = ["#FF0000","#FF7D00","#FFFF00","#00FF00","#0000FF","#00FFFF","#FF00FF"];
		var colorIndex = 0;
		var interval ;
		
    	$(function(){
    		//建立WebSocket连接
    		createWebSocketClient(sessionId,userId,userName);
    		
            screenFuc();
            (window.onresize = function () {
                screenFuc();
            })();
            //未读信息数量为空时
            var totalNum = $(".chat-message-num").html();
            if (totalNum == "") {
                $(".chat-message-num").css("padding", 0);
            }
            $(".message-num").each(function () {
                var wdNum = $(this).html();
                if (wdNum == "") {
                    $(this).css("padding", 0);
                }
            });

            //打开/关闭聊天框
            $(".chatBtn").click(function () {
                $(".chatBox").toggle(10);
            })
            $(".chat-close").click(function () {
                $(".chatBox").toggle(10);
            })

            //返回列表
            $(".chat-return").click(function () {
                $(".chatBox-head-one").toggle(1);
                $(".chatBox-head-two").toggle(1);
                $(".chatBox-list").fadeToggle(1);
                $(".chatBox-kuang").fadeToggle(1);
            });

            //      发送信息
            $("#chat-fasong").click(function () {
                var textContent = $(".div-textarea").html().replace(/[\n\r]/g, '<br>')
                if (textContent != "") {
                    $(".chatBox-content-demo").append("<div class=\"clearfloat\">" +
                        "<div class=\"author-name\"><small class=\"chat-date\">"+CurentTime()+"</small> </div> " +
                        "<div class=\"right\"> <div class=\"chat-message\"> " + textContent + " </div> " +
                        "<div class=\"chat-avatars\"><img src=\""+me.img+"\" alt=\"头像\" /></div> </div> </div>");
                    //发送后清空输入框
                    $(".div-textarea").html("");
                    
                    //webSocket 发送消息
                    var sendMsg = {};
                    sendMsg.id = "1";
                    sendMsg.msgType = "0";
                    sendMsg.from = userId;
                    sendMsg.to = $("#toUserId").val();
                    sendMsg.data = textContent;
                    sendMsg.type = "2";
                    send(sendMsg);
                    
                    //聊天框默认最底部
                    $(document).ready(function () {
                        $("#chatBox-content-demo").scrollTop($("#chatBox-content-demo")[0].scrollHeight);
                    });
                }
            });

            //      发送表情
            $("#chat-biaoqing").click(function () {
                $(".biaoqing-photo").toggle();
            });
            $(document).click(function () {
                $(".biaoqing-photo").css("display", "none");
            });
            $("#chat-biaoqing").click(function (event) {
                event.stopPropagation();//阻止事件
            });
            $(".emoji-picker-image").each(function () {
                $(this).click(function () {
                    var bq = $(this).parent().html();
                    $(".chatBox-content-demo").append("<div class=\"clearfloat\">" +
                        "<div class=\"author-name\"><small class=\"chat-date\">2017-12-02 14:26:58</small> </div> " +
                        "<div class=\"right\"> <div class=\"chat-message\"> " + bq + " </div> " +
                        "<div class=\"chat-avatars\"><img src=\"img/icon01.png\" alt=\"头像\" /></div> </div> </div>");
                    //发送后关闭表情框
                    $(".biaoqing-photo").toggle();
                    //聊天框默认最底部
                    $(document).ready(function () {
                        $("#chatBox-content-demo").scrollTop($("#chatBox-content-demo")[0].scrollHeight);
                    });
                })
            });
    	})

    	function createWebSocketClient(sessionId,userId,userName){
    		var webSocketLogin  = "{\"id\":\""+sessionId+"\",\"userId\":\""+userId+"\",\"userName\":\""+userName+"\"}";
            if (!window.WebSocket) {
                window.WebSocket = window.MozWebSocket;
            }
            if (window.WebSocket) {
                socket = new WebSocket("ws://localhost:7397?webSocketLogin="+webSocketLogin);
                
                socket.onmessage = function(event) {
                	var dataJson = JSON.parse(event.data);
                    var ta = $("#responseText");
                    if(dataJson.type == -1){ //是服务器消息
                    	ta.val(ta.val() + '\n' + dataJson.data);
                    }else  if(dataJson.type == 0){ //是服务器推送的联系人列表消息
                    	//ta.val(ta.val() + '\n目前列表人数：' + (Number(dataJson.data.length) + Number(1)));
                    	//刷新成最新的联系人列表
                    	createNewContactsList(dataJson.data);
           				
                    }else if(dataJson.type == 1){//服务器推送过来最新联系人列表
                    	//ta.val(ta.val() + '\n有人上线了：' + JSON.stringify(dataJson.data));
                    	//刷新成最新的联系人列表
                    	createNewContactsList(dataJson.data);
                    	//上线渐变色
                    	interval = setInterval("changeColor("+colorIndex+","+interval+")",800);
    					
                    }else if(dataJson.type == 2){ //一对一聊天 有人发送消息给你
                    	ta.val(ta.val() + '\n有人发送消息给你：' + JSON.stringify(dataJson));
                    	var textContent = $(".div-textarea").html().replace(/[\n\r]/g, '<br>')
                    	var img ;
                    	for(var i = 0 ; i < contactsList.length ; i++){
                    		if(dataJson.from == contactsList[i].id){
                    			img = contactsList[i].img;
                    		}
                    	}
                        $(".chatBox-content-demo").append(
                        	"<div class='clearfloat'>"+
			                           "<div class='author-name'>"+
		                           "<small class='chat-date'>"+dataJson.date+"</small>"+
		                       "</div>"+
		                       "<div class='left'>"+
		                           "<div class='chat-avatars'><img src='"+img+"' alt='头像'/></div>"+
		                           "<div class='chat-message'>"+
		                           	dataJson.data+
		                           "</div>"+
		                       "</div>"+
		                   "</div>");
                    	//聊天框默认最底部
                        $(document).ready(function () {
                            $("#chatBox-content-demo").scrollTop($("#chatBox-content-demo")[0].scrollHeight);
                        });
                    }else if(dataJson.type == 3){ //拉取到一对一聊天记录后 展示出来
                    	ta.val(ta.val() + '\n一对一聊天记录：' + JSON.stringify(dataJson.data));
                    	var oneToOneHistoryMessage = "";
                    	$(".chatBox-content-demo").empty();
                    	var temp = JSON.parse(dataJson.data);
                    	for(var i = 0 ; i < temp.length ; i ++){
                    		if(temp[i].from == userId){  //说明是自己发送的历史消息  应该右边靠齐显示
                    			oneToOneHistoryMessage +="<div class=\"clearfloat\">" +
                                "<div class=\"author-name\"><small class=\"chat-date\">"+temp[i].date+"</small> </div> " +
                                "<div class=\"right\"> <div class=\"chat-message\"> " + temp[i].data + " </div> " +
                                "<div class=\"chat-avatars\"><img src=\""+contactsListForPicture.get(temp[i].from)+"\" alt=\"头像\" /></div> </div> </div>";
                    		}else{
                    			oneToOneHistoryMessage +="<div class='clearfloat'>"+
                    			"<div class='author-name'><small class='chat-date'>"+temp[i].date+"</small></div>"+
	                       		"<div class='left'><div class='chat-avatars'><img src='"+contactsListForPicture.get(temp[i].from)+"' alt='头像'/></div>"+
	                           "<div class='chat-message'>"+temp[i].data+"</div></div></div>";
                    		}
        	        	}
                    	console.log(contactsListForPicture);
                    	console.log(contactsListForPicture.get(10003));
                    	$(".chatBox-content-demo").append(oneToOneHistoryMessage);
                    	//聊天框默认最底部
                        $(document).ready(function () {
                            $("#chatBox-content-demo").scrollTop($("#chatBox-content-demo")[0].scrollHeight);
                        });
                    }
                };
                
                socket.onopen = function(event) {
                	$("#responseText").val("连接开启!");
                };
                
                socket.onclose = function(event) {
                	$("#responseText").val("连接被关闭!");
                };
                
            } else {
                alert("你的浏览器不支持！");
            }	
    	}
    	
    	 function send(message) {
            if (!window.WebSocket) {
                return;
            }
            if (socket.readyState == WebSocket.OPEN) {
          	    socket.send(JSON.stringify(message));
            } else {
                alert("连接没有开启.");
            }
        }

    	 function screenFuc() {
            var topHeight = $(".chatBox-head").innerHeight();//聊天头部高度
            //屏幕小于768px时候,布局change
            var winWidth = $(window).innerWidth();
            if (winWidth <= 768) {
                var totalHeight = $(window).height(); //页面整体高度
                $(".chatBox-info").css("height", totalHeight - topHeight);
                var infoHeight = $(".chatBox-info").innerHeight();//聊天头部以下高度
                //中间内容高度
                $(".chatBox-content").css("height", infoHeight - 46);
                $(".chatBox-content-demo").css("height", infoHeight - 46);

                $(".chatBox-list").css("height", totalHeight - topHeight);
                $(".chatBox-kuang").css("height", totalHeight - topHeight);
                $(".div-textarea").css("width", winWidth - 106);
            } else {
                $(".chatBox-info").css("height", 495);
                $(".chatBox-content").css("height", 448);
                $(".chatBox-content-demo").css("height", 448);
                $(".chatBox-list").css("height", 495);
                $(".chatBox-kuang").css("height", 495);
                $(".div-textarea").css("width", 260);
            }
        }
        
        //      发送图片
        function selectImg(pic) {
            if (!pic.files || !pic.files[0]) {
                return;
            }
            var reader = new FileReader();
            reader.onload = function (evt) {
                var images = evt.target.result;
                
                $(".chatBox-content-demo").append("<div class=\"clearfloat\">" +
                    "<div class=\"author-name\"><small class=\"chat-date\">"+CurentTime()+"</small> </div> " +
                    "<div class=\"right\"> <div class=\"chat-message\"><img src=" + images + "></div> " +
                    "<div class=\"chat-avatars\"><img src=\"img/icon01.png\" alt=\"头像\" /></div> </div> </div>");
                //聊天框默认最底部
                $(document).ready(function () {
                    $("#chatBox-content-demo").scrollTop($("#chatBox-content-demo")[0].scrollHeight);
                });
            };
            reader.readAsDataURL(pic.files[0]);
        }
        
     	 //进聊天页面
        function addClickForContacts(){
            $(".chat-list-people").each(function () {
                $(this).click(function () {
                    var n = $(this).index();
                    $(".chatBox-head-one").toggle();
                    $(".chatBox-head-two").toggle();
                    $(".chatBox-list").fadeToggle();
                    $(".chatBox-kuang").fadeToggle();
					
                    //传id
                    $("#toUserId").val($(this).children(".chat-name").children("input").val());
                    //传名字
                    $(".ChatInfoName").text($(this).children(".chat-name").children("p").eq(0).text().split("  ")[0]);
                    //传头像
                    $(".ChatInfoHead>img").attr("src", $(this).children().eq(0).children("img").attr("src"));
                    
                    //聊天框默认最底部
                    $(document).ready(function () {
                        $("#chatBox-content-demo").scrollTop($("#chatBox-content-demo")[0].scrollHeight);
                    });
                    
                    //发送到后台拉取最近三天的聊天记录
                    var sendMsg = {};
                    sendMsg.id = "1";
                    sendMsg.from = userId;
                    sendMsg.to = $("#toUserId").val();
                    sendMsg.type = "3";
                    sendMsg.msgDate = "0";
                    send(sendMsg);
                })
            });
        }
        
     	 //刷新成最新的联系人列表
    	function createNewContactsList(newContactsList){
    		var contactsTable = "";
       		for(var i = 0 ; i < newContactsList.length ; i ++){
       			if(newContactsList[i].id  != userId){
       				contactsTable += "<div class='chat-list-people'>";
           			if(newContactsList[i].isOnline ==  true){
           				contactsTable += "<div><img src='"+newContactsList[i].img+"' alt='头像'/></div>";
           				contactsListForPicture.set(newContactsList[i].id,newContactsList[i].img);
           			}else{
           				contactsTable += "<div><img src='"+newContactsList[i].grayImg+"' alt='头像'/></div>";
           				contactsListForPicture.set(newContactsList[i].id,newContactsList[i].grayImg);
           			} 
           			contactsTable += "<div class='chat-name'>";
           			contactsTable += "<input type='hidden' value='"+newContactsList[i].id+"'/>";
           			contactsTable += "<p>"+newContactsList[i].nickName+"<span id='contactsName'>  ("+newContactsList[i].name+")</span></p>";
           			contactsTable += "</div>";
           			contactsTable += "<div class='message-num'>10</div>";
           			contactsTable += "</div>";
       			}else{
       				me = newContactsList[i];
       				contactsListForPicture.set(newContactsList[i].id,newContactsList[i].img);
       			}
           	}
   			$("#contactsTable").html(contactsTable);
   			//联系人列表添加点击事件
            addClickForContacts();
   			//js 保存最新联系人列表
            contactsList = newContactsList;
     	 }
     	 
    	function changeColor() {
    		if(colorIndex == colors.length){
    			colorIndex = 0;
    			$(".chat-list-people").children(".chat-name").children("p").eq(0).css("color","black");
    			clearInterval(interval);
    		}else{
    			$(".chat-list-people").children(".chat-name").children("p").eq(0).css("color",colors[colorIndex]);
    			colorIndex++;
    		}	   	
    	}

			
        function CurentTime(){
            var now = new Date();
            var year = now.getFullYear();       //年
            var month = now.getMonth() + 1;     //月
            var day = now.getDate();            //日
            var hh = now.getHours();            //时
            var mm = now.getMinutes();          //分
            var ss = now.getSeconds();           //秒
            
            var clock = year + "-";
            
            if(month < 10)
                clock += "0";
            clock += month + "-";
            
            if(day < 10)
                clock += "0";
            clock += day + " ";
            
            if(hh < 10)
                clock += "0";
            clock += hh + ":";
            if (mm < 10) 
            	clock += '0'; 
            clock += mm + ":"; 
            if (ss < 10) clock += '0'; 
            clock += ss; 
            return clock; 
    	}

</script>
</head>
<body>
    <form onsubmit="return false;">
        <div>
	        <h3>输出消息：</h3>
	        <textarea id="responseText" style="width: 300px; height: 600px;" readonly></textarea>
	        <input type="button" onclick="javascript:document.getElementById('responseText').value=''" value="清空">
        </div>
    </form>
    
    <div class="chatContainer">
    	<!-- 最小化时 图标 开始 -->
	    <div class="chatBtn">
	        <i class="iconfont icon-xiaoxi1"></i>
	    </div>
	    <!-- 最小化时 图标 结束 -->
	    <!-- 最小化时 未读消息条数 -->
	    <div class="chat-message-num"></div>
	    
	    
	    <div class="chatBox" ref="chatBox">
	    	<!-- 聊天主体展开时的头部 开始 -->
	        <div class="chatBox-head">
	            <div class="chatBox-head-one">
	                联系人
	                <div class="chat-close" style="margin: 10px 10px 0 0;font-size: 14px">关闭</div>
	            </div>
	            <div class="chatBox-head-two">
	                <div class="chat-return">返回</div>
	                <div class="chat-people">
	                    <div class="ChatInfoHead">
	                        <img src="" alt="头像"/>
	                    </div>
	                    <div class="ChatInfoName"></div>
	                    <input type='hidden'  id='toUserId' value=''/>
	                </div>
	                <div class="chat-close">关闭</div>
	            </div>
	        </div>
	        <!-- 聊天主体展开时的头部 结束 -->
	        
	        <div class="chatBox-info">
	        	<!-- 聊天主体展开时的联系人列表 开始 -->
	            <div class="chatBox-list" ref="chatBoxlist" id="contactsTable">
	                
	            </div>
	            <!-- 聊天主体展开时的联系人列表 结束 -->
	            
	            <div class="chatBox-kuang" ref="chatBoxkuang">
	            	<!-- 聊天窗口展示框体  开始 -->
	                <div class="chatBox-content">
	                    <div class="chatBox-content-demo" id="chatBox-content-demo">
	                        <%-- <div class="clearfloat">
	                            <div class="author-name">
	                                <small class="chat-date">2017-12-02 14:26:58</small>
	                            </div>
	                            <div class="left">
	                                <div class="chat-avatars"><img src="<%=path %>/img/icon01.png" alt="头像"/></div>
	                                <div class="chat-message">
	                                    给你看张图
	                                </div>
	                            </div>
	                        </div>
	
	                        <div class="clearfloat">
	                            <div class="author-name">
	                                <small class="chat-date">2017-12-02 14:26:58</small>
	                            </div>
	                            <div class="left">
	                                <div class="chat-avatars"><img src="<%=path %>/img/icon01.png" alt="头像"/></div>
	                                <div class="chat-message">
	                                    <img src="<%=path %>/img/1.png" alt="">
	                                </div>
	                            </div>
	                        </div>
	
	                        <div class="clearfloat">
	                            <div class="author-name">
	                                <small class="chat-date">2017-12-02 14:26:58</small>
	                            </div>
	                            <div class="right">
	                                <div class="chat-message">嗯，适合做壁纸</div>
	                                <div class="chat-avatars"><img src="<%=path %>/img/icon02.png" alt="头像"/></div>
	                            </div>
	                        </div>
	                    </div> --%>
	                </div>
	                <!-- 聊天窗口展示框体  结束-->
	                
	                <!-- 聊天窗口编辑框  开始 -->
	                <div class="chatBox-send">
	                    <div class="div-textarea" contenteditable="true"></div>
	                    <div>
	                        <button id="chat-biaoqing" class="btn-default-styles">
	                            <i class="iconfont icon-biaoqing"></i>
	                        </button>
	                        <label id="chat-tuxiang" title="发送图片" for="inputImage" class="btn-default-styles">
	                            <input type="file" onchange="selectImg(this)" accept="image/jpg,image/jpeg,image/png"
	                                   name="file" id="inputImage" class="hidden">
	                            <i class="iconfont icon-tuxiang"></i>
	                        </label>
	                        <button id="chat-fasong" class="btn-default-styles"><i class="iconfont icon-fasong"></i>
	                        </button>
	                    </div>
	                    <div class="biaoqing-photo">
	                        <ul>
	                            <li><span class="emoji-picker-image" style="background-position: -9px -18px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -40px -18px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -71px -18px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -102px -18px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -133px -18px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -164px -18px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -9px -52px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -40px -52px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -71px -52px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -102px -52px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -133px -52px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -164px -52px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -9px -86px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -40px -86px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -71px -86px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -102px -86px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -133px -86px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -164px -86px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -9px -120px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -40px -120px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -71px -120px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -102px -120px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -133px -120px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -164px -120px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -9px -154px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -40px -154px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -71px -154px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -102px -154px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -133px -154px;"></span></li>
	                            <li><span class="emoji-picker-image" style="background-position: -164px -154px;"></span></li>
	                        </ul>
	                    </div>
	                </div>
	                <!-- 聊天窗口编辑框  结束 -->
	            </div>
	        </div>
	    </div>
	</div>
	
	
</body>
</html>