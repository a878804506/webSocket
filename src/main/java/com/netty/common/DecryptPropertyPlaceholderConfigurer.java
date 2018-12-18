package com.netty.common;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public class DecryptPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer{
	/**
	 * 重写父类方法，解密指定属性名对应的属性值
	 */
	@Override
	protected String convertProperty(String propertyName,String propertyValue){
		return propertyValue;
	}
}
