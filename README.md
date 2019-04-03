# webSocket

webSocket网页聊天项目（旧版）

本项目（spring）使用netty实现了webSocket聊天功能，后台通过配置redis以及自定义的通信报文，实现文本消息、表情的实时发送、离线消息提醒、未读消息标识、查看历史记录等等
但是未实现发送和接收图片和文件功能

本项目由于架构比较臃肿，已经改造为springboot的新项目，详见我的另一个项目：[NettyWebCaht](https://github.com/a878804506/NettyWebCaht)

由于没有放配置文件，所以需要添加配置文件进行使用
