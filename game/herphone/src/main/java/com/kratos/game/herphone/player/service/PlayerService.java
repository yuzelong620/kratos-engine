package com.kratos.game.herphone.player.service;

import com.kratos.engine.framework.crud.ICrudService;
import com.kratos.engine.framework.wechat.bean.ResWechatMiniOpenId;
import com.kratos.game.herphone.player.domain.Player;
import com.kratos.game.herphone.player.message.ReqPlayerTencentLogin;
import com.kratos.game.herphone.player.message.ResPlayerLogin;

import java.util.List;

public interface PlayerService extends ICrudService<Long, Player> {
    /**
     * 游客登录
     * @return 是否为创建用户
     */
    ResPlayerLogin guestLogin();

    /**
     * 新的qq登录
     * @param request
     * @return
     */
    ResPlayerLogin tencentLogin2(ReqPlayerTencentLogin request);

    /**
     * 根据微信验证code登录
     * @param code 信验证code登录
     * @return 是否为创建用户
     */
    ResPlayerLogin wechatLogin(String code);

    /**
     * 微信小程序微信登录
     * @param code
     * @return
     */
    ResWechatMiniOpenId wechatMiniLogin(String code);

    List<Player> listInitPlayer(int page);
}
