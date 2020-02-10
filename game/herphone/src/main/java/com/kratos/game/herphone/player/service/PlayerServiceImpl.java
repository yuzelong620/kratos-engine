package com.kratos.game.herphone.player.service;

import com.kratos.game.herphone.common.CommonService;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.globalgame.auto.json.Achievement_Json;
import com.globalgame.auto.json.CreatRole_Json;
import com.globalgame.auto.json.GameCatalog_Json;
import com.globalgame.auto.json.GameParams_Json;
import com.globalgame.auto.json.Item_Json;
import com.globalgame.auto.json.Tag_Json;
import com.kratos.engine.framework.common.CommonConstant;
import com.kratos.engine.framework.common.utils.IdGenerator;
import com.kratos.engine.framework.common.utils.JedisUtils;
import com.kratos.engine.framework.common.utils.StringHelper;
import com.kratos.engine.framework.crud.BaseCrudService;
import com.kratos.engine.framework.crud.Param;
import com.kratos.engine.framework.net.socket.exception.BusinessException;
import com.kratos.engine.framework.tencent.TencentManager;
import com.kratos.engine.framework.tencent.bean.ResTencentUserInfo;
import com.kratos.engine.framework.wechat.WechatManager;
import com.kratos.engine.framework.wechat.bean.ResAccessToken;
import com.kratos.engine.framework.wechat.bean.ResWechatUserInfo;
import com.kratos.game.herphone.bag.dao.BagDao;
import com.kratos.game.herphone.bag.entity.BagEntity;
import com.kratos.game.herphone.bag.service.BagService;
import com.kratos.game.herphone.cache.AppCache;
import com.kratos.game.herphone.config.GlobalData;
import com.kratos.game.herphone.json.JsonCacheManager;
import com.kratos.game.herphone.json.datacache.AchievementCache;
import com.kratos.game.herphone.json.datacache.CreatRoleCache;
import com.kratos.game.herphone.json.datacache.GameCatalogCache;
import com.kratos.game.herphone.json.datacache.GameParamsCache;
import com.kratos.game.herphone.json.datacache.ItemCache;
import com.kratos.game.herphone.json.datacache.TagCache;
import com.kratos.game.herphone.player.PlayerContext;
import com.kratos.game.herphone.player.bean.PlayerPage;
import com.kratos.game.herphone.player.bean.PlayerScoreBean;
import com.kratos.game.herphone.player.bean.RanKingData;
import com.kratos.game.herphone.player.bean.ResPlayerPowerData;
import com.kratos.game.herphone.player.domain.Player;
import com.kratos.game.herphone.player.message.ReqPlayerEdit;
import com.kratos.game.herphone.player.message.ReqPlayerTencentLogin;
import com.kratos.game.herphone.player.message.ReqPlayerWechatLogin;
import com.kratos.game.herphone.player.message.ResPlayerLogin;
import com.kratos.game.herphone.player.message.ResPlayerProfile;
import com.kratos.game.herphone.player.message.ResRankPlayer;
import com.kratos.game.herphone.player.web.GMDataChange;
import com.kratos.game.herphone.playerDynamic.dao.PlayerDynamicDao;
import com.kratos.game.herphone.playerDynamic.entity.PlayerDynamicEntity;
import com.kratos.game.herphone.playerDynamic.service.PlayerDynamicService;
import com.kratos.game.herphone.scheduled.ScheduledService;
import lombok.extern.log4j.Log4j;

import com.alibaba.fastjson.JSON;
import com.github.kevinsawicki.http.HttpRequest;
import java.util.Map;
import java.util.HashMap;

import com.kratos.game.herphone.common.CommonCost;
import com.kratos.game.herphone.common.CommonCost.ChannelId;
import com.kratos.game.herphone.tencent.dao.UnionIdDao;
import com.kratos.game.herphone.tencent.entity.UnionIdEntity;
import com.kratos.game.herphone.tencent.service.UnionIdService;
import com.alibaba.fastjson.JSONObject;
@Log4j
@Component
public class PlayerServiceImpl extends BaseCrudService<Long, Player> implements PlayerService {

    @Autowired
    private RoleIdService roleIdService;//角色id生成service

    /**
     * 游客登录
     * @return
     */
    @Override
    public ResPlayerLogin guestLogin() {
        //注册游客
        Player player = this.guestRegister();
        //实例化返回值对象
        ResPlayerLogin resPlayerLogin = new ResPlayerLogin();
        //设置已创建标致
        resPlayerLogin.setCreate(true);
//        OnFire.fire(new PlayerLoginEvent(player));
        //设置token
        resPlayerLogin.setToken(player.getToken());
        resPlayerLogin.setPlayer(new ResPlayerProfile(player));
        return resPlayerLogin;
    }

    /**
     *  游客注册
     */

    private Player guestRegister() {
        Player player = new Player();
        player.setRoleId(roleIdService.getNextRoleId());
        try {
            player.setNickName(new String(Base64.getEncoder().encode(("guest" + player.getRoleId()).getBytes()), CommonConstant.UTF8));
        } catch (UnsupportedEncodingException e) {
            log.error("", e);
        }
        player.setGender("1");
        return this.register(player);
    }

    /**
     * 玩家角色注册
     * @param player
     * @return
     */
    private Player register(Player player) {
        //设置初始电量
        return register(player,100);

    }

    /**
     * 玩家角色注册
     * @param player
     * @param power 初始电量值
     * @return
     */
    public Player register(Player player,int power) {

        player.setId(IdGenerator.getNextId());
        player.setToken(UUID.randomUUID().toString());
        player.setLastRecoverPowerTime(System.currentTimeMillis());
        player.setPower(power);
        player.setExploration(0);
        player.setAchievement(0);
        player.setRegister(System.currentTimeMillis());
        this.cacheAndPersist(player.getId(), player);
        return player;
    }
}
