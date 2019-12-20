package com.kratos.game.herphone.player.service;

import java.util.List;
import java.util.Map;

import com.globalgame.auto.json.Achievement_Json;
import com.kratos.engine.framework.crud.ICrudService;
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
import com.kratos.game.herphone.player.bean.PlayerProperty;
import com.kratos.game.herphone.player.bean.PlayerAllBean;
import com.kratos.game.herphone.player.bean.PlayerAstrictBean;
import com.kratos.game.herphone.player.bean.PlayerIdBean;
public interface PlayerService extends ICrudService<Long, Player> {
    /**
     * 根据微信验证code登录
     * @param code 信验证code登录
     * @return 是否为创建用户
     */
    ResPlayerLogin wechatLogin(String code);

    /**
     * QQ登录
     * @param request 请求
     * @return 是否为创建用户
     */
    ResPlayerLogin tencentLogin(ReqPlayerTencentLogin request);

    /**
     * 游客登录
     * @return 是否为创建用户
     */
    ResPlayerLogin guestLogin();

    /**
     * 根据token查询
     * @param token token
     * @return 用户
     */
    Player findByToken(String token);

    /**
     * 重命名
     * @param reqPlayerEdit 用户信息
     */
    Player edit(ReqPlayerEdit reqPlayerEdit);

    /**
     * 获取用户信息
     */
    ResPlayerProfile getProfile();

    /**
     * 授权
     * @param request 请求
     * @return 玩家
     */
    Player auth(ReqPlayerEdit request);


    /**
     * 访问首页
     */
    void index();
    
    /**根据角色Id查找player*/
    Long getPlayerByRoleId(String roleId);
    

    /**
     * 设置电量
     * @param power 电量
     */
    ResPlayerProfile setPower(Integer power);

    /**
     * gm设置电量
     * @param name 玩家名
     * @param power 电量
     * @return 用户
     */
    ResPlayerProfile gmSetPower(String name, Integer power);

    /**
     * 绑定微信
     * @param request 请求参数
     * @return 用户信息
     */
    ResPlayerProfile bindWechat(ReqPlayerWechatLogin request);

      /*  * 每日重置
     * @param player 用户
     */
	void checkDailyReset(Player player);
    /** 
     * 绑定QQ(老接口)
     * @param request 请求参数
     * @return 用户信息
     */
    ResPlayerProfile bindTencent(ReqPlayerTencentLogin request);
    /**
     * 绑定QQ 新的的方法, request.channelId 参数用于 不同appid.  
     * @param request 请求参数
     * @return 用户信息
     */
    ResPlayerProfile bindTencent2(ReqPlayerTencentLogin request);


    /**
     * 新的qq登录
     * @param request
     * @return
     */
    ResPlayerLogin tencentLogin2(ReqPlayerTencentLogin request);

	/**
	 * 创建机器人
	 */
	void creatRobot();



	/**
	 * 查询数据库违规总数
	 * */
	@Deprecated
	public long badcount();

    public List<Player> listInitPlayer(int page);

}
