package com.kratos.game.herphone.player.service;

import com.kratos.engine.framework.crud.ICrudService;
import com.kratos.game.herphone.player.domain.Player;
import com.kratos.game.herphone.player.message.ResPlayerLogin;
public interface PlayerService extends ICrudService<Long, Player> {
    /**
     * 游客登录
     * @return 是否为创建用户
     */
    ResPlayerLogin guestLogin();
}
