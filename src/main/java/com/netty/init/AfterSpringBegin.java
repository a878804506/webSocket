package com.netty.init;

import java.util.Timer;
import java.util.TimerTask;



import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
/**
 * 
 * spring加载后改方法的子类
 * */
public abstract class AfterSpringBegin extends TimerTask  implements ApplicationListener<ContextRefreshedEvent>{

	public void onApplicationEvent(ContextRefreshedEvent event) {
		// TODO Auto-generated method stub
		if(event.getApplicationContext().getParent() ==null){
			
			Timer timer = new Timer();
			timer.schedule(this, 0);
		}
	}

}
