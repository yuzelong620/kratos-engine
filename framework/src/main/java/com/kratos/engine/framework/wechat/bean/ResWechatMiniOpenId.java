package com.kratos.engine.framework.wechat.bean;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResWechatMiniOpenId {
	private String openid;
	private String session_key;
	private String unionid;
	private String errcode;
	private String errmsg;
}
