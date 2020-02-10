package com.kratos.game.herphone.player.service;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import com.globalgame.auto.json.GameParams_Json;
import com.kratos.engine.framework.wechat.WechatManager;
import com.kratos.engine.framework.wechat.bean.ResAccessToken;
import com.kratos.engine.framework.wechat.bean.ResWechatMiniOpenId;
import com.kratos.engine.framework.wechat.bean.ResWechatUserInfo;
import com.kratos.game.herphone.bag.dao.BagDao;
import com.kratos.game.herphone.bag.entity.BagEntity;
import com.kratos.game.herphone.bag.service.BagService;
import com.kratos.game.herphone.json.JsonCacheManager;
import com.kratos.game.herphone.json.datacache.GameParamsCache;
import com.kratos.game.herphone.playerDynamic.dao.PlayerDynamicDao;
import com.kratos.game.herphone.playerDynamic.service.PlayerDynamicService;
import com.kratos.game.herphone.playerOnline.service.PlayerLoginTimeService;
import com.kratos.game.herphone.tencent.service.UnionIdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.kratos.engine.framework.common.CommonConstant;
import com.kratos.engine.framework.common.utils.IdGenerator;
import com.kratos.engine.framework.crud.BaseCrudService;
import com.kratos.engine.framework.crud.Param;
import com.kratos.engine.framework.net.socket.exception.BusinessException;
import com.kratos.engine.framework.tencent.bean.ResTencentUserInfo;
import com.kratos.game.herphone.player.domain.Player;
import com.kratos.game.herphone.player.message.ReqPlayerTencentLogin;
import com.kratos.game.herphone.player.message.ResPlayerLogin;
import com.kratos.game.herphone.player.message.ResPlayerProfile;
import com.kratos.game.herphone.playerDynamic.entity.PlayerDynamicEntity;
import lombok.extern.log4j.Log4j;

import com.alibaba.fastjson.JSON;
import com.github.kevinsawicki.http.HttpRequest;
import java.util.Map;
import java.util.HashMap;
import com.kratos.game.herphone.common.CommonCost.ChannelId;
import com.kratos.game.herphone.tencent.dao.UnionIdDao;
import com.kratos.game.herphone.tencent.entity.UnionIdEntity;
import com.alibaba.fastjson.JSONObject;

import javax.persistence.Query;

@Log4j
@Component
public class PlayerServiceImpl extends BaseCrudService<Long, Player> implements PlayerService {

    @Autowired
    private RoleIdService roleIdService;//角色id生成service
    @Autowired
    private WechatManager wechatManager;
    @Autowired
    UnionIdDao unionIdDao;
    @Autowired
    UnionIdService unionIdService;
    @Autowired
    PlayerDynamicService playerDynamicService;
    @Autowired
    PlayerDynamicDao playerDynamicDao;
    @Autowired
    private PlayerLoginTimeService playerLoginTimeService;
    @Autowired
    private BagService bagService;
    @Autowired
    private BagDao bagDao;
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

    /**
     * QQ登录
     * @param request
     * @return
     */
    @Override
    public ResPlayerLogin tencentLogin2(ReqPlayerTencentLogin request) {
        ResPlayerLogin resPlayerLogin = new ResPlayerLogin();
        checkUnionidParams(request);//检查unionid
        String openId = request.getOpenId();
        Player player=null;
        //根据 unionid 获取player
        UnionIdEntity unionidEntity=unionIdDao.findByUnionId(request.getUnionid());
        if(unionidEntity!=null){
            player=get(unionidEntity.getPlayerId());
        }
        //根据openid获取player
        if(player==null){
            player = findByTencentOpenId(openId);
        }
        if (player == null) {
            player = new Player();
            player.setRoleId(roleIdService.getNextRoleId());
            //
            player.setTencentOpenid(openId);
            ResTencentUserInfo userInfo =getUserInfoNew(request.getChannelId(),request.getAccessToken(),openId);
            if(!userInfo.getRet().equals("0")){
                throw new BusinessException("获取qq信息失败");
            }
            player.setGender(userInfo.getGender().equals("男") ? "1" : "2");
            if (userInfo.getNickname() != null) {
                try {
                    player.setNickName(new String(Base64.getEncoder().encode(userInfo.getNickname().getBytes()), CommonConstant.UTF8));
                } catch (UnsupportedEncodingException e) {
                    log.error("", e);
                }
            }
            //获取QQ头像 40*40像素
            player.setAvatarUrl(userInfo.getFigureurl_qq_1());
            player.setIsGuest(0);
            player=this.register(player);
            resPlayerLogin.setCreate(true);
        }else if (player.getIsGuest() == 1) {
            player.setIsGuest(0);
            this.cacheAndPersist(player.getId(), player);
        }
        //如果第一次使用unionid
        if(unionidEntity==null){
            unionIdService.save(request.getUnionid(),player.getId());
        }
        loginExecute(player);
        resPlayerLogin.setToken(player.getToken());
        resPlayerLogin.setPlayer(new ResPlayerProfile(player));
        PlayerDynamicEntity playerDynamicEntity = playerDynamicDao.findByID(player.getId());
        if(playerDynamicEntity != null){
            ResPlayerProfile resPlayerProfile = resPlayerLogin.getPlayer();
            resPlayerProfile.setAchievementTags(playerDynamicEntity.getAchievementTags());
            resPlayerProfile.setAvatarFrame(playerDynamicEntity.getAvatarFrame());
        }
        return resPlayerLogin;
    }

    /**
     * 微信小程序登录
     * @param code
     * @return
     */
    @Override
    public ResWechatMiniOpenId wechatMiniLogin(String code) {
        ResWechatMiniOpenId wechatMiniOpenId = wechatManager.getWechatMiniService().getWechatMiniOpenId(code);
        return wechatMiniOpenId;
    };

    /**
     * App微信登录
     * @param code 信验证code登录
     * @return
     */
    @Override
    public ResPlayerLogin wechatLogin(String code) {
        String openId;
        ResPlayerLogin resPlayerLogin = new ResPlayerLogin();
        ResAccessToken resAccessToken = wechatManager.getWechatService().getAppAccessToken(code);
        openId = resAccessToken.getOpenid();
        String accessToken = resAccessToken.getAccess_token();
        if (accessToken == null) {
            log.error("错误码："+resAccessToken.getErrcode()+",错误信息:"+resAccessToken.getErrmsg());
            throw new BusinessException("微信登录失败");
        }
        Player player = findByWechatOpenId(openId);
        if (player == null) {
            player = this.wechatRegister(accessToken, openId);
            resPlayerLogin.setCreate(true);
        }else if (player.getIsGuest() == 1) {
            player.setIsGuest(0);
            this.cacheAndPersist(player.getId(), player);
        }
        loginExecute(player);
        resPlayerLogin.setToken(player.getToken());
        resPlayerLogin.setPlayer(new ResPlayerProfile(player));
        PlayerDynamicEntity playerDynamicEntity = playerDynamicDao.findByID(player.getId());
        if(playerDynamicEntity != null){
            ResPlayerProfile resPlayerProfile = resPlayerLogin.getPlayer();
            resPlayerProfile.setAchievementTags(playerDynamicEntity.getAchievementTags());
            resPlayerProfile.setAvatarFrame(playerDynamicEntity.getAvatarFrame());
        }
        return resPlayerLogin;
    }

    /**
     * 根据openId查询用户
     * @param openId
     * @return
     */
    @Transactional(readOnly = true)
    public Player findByWechatOpenId(String openId) {
        List<Player> resultList = this.findByParams(Param.equal("wechatOpenid", openId));
        if (resultList == null || resultList.isEmpty()) {
            return null;
        }
        return resultList.get(0);
    }

    /**
     * 根据accessToken,openId注册新用户
     * @param accessToken
     * @param openid
     * @return
     */
    private Player wechatRegister(String accessToken, String openid) {
        Player player = new Player();
        player.setRoleId(roleIdService.getNextRoleId());
        player.setIsGuest(0);
        this.setWechatInfo(player, accessToken, openid);
        return this.register(player);
    }

    /**
     * 设置微信信息
     * @param player
     * @param accessToken
     * @param openid
     */
    private void setWechatInfo(Player player, String accessToken, String openid) {
        player.setWechatOpenid(openid);
        ResWechatUserInfo userInfo = wechatManager.getWechatService().getUserInfo(accessToken, openid);
        player.setGender(userInfo.getSex());
        if (userInfo.getNickname() != null) {
            try {
                player.setNickName(new String(Base64.getEncoder().encode(userInfo.getNickname().getBytes()), CommonConstant.UTF8));
            } catch (UnsupportedEncodingException e) {
                log.error("", e);
            }
        }
        player.setAvatarUrl(userInfo.getHeadimgurl());
    }

    /**检查 unionid ,如果不存在调用 qq接口获取 unionid*/
    public void checkUnionidParams(ReqPlayerTencentLogin req){
        if(req.getUnionid()!=null){
            return;
        }
        try{
            String url=getUnionUrl(req.getAccessToken());
            String result = HttpRequest.get(url).accept("application/json").body();
            if(result==null){
                log.error("获取unionid 失败，accessToken:"+req.getAccessToken());
                return;
            }
            if(result.contains("callback( ")){
                result=result.replace("callback( ", "");
            }
            if(result.contains(" );")){
                result=result.replace(" );", "");
            }
            JSONObject json=JSONObject.parseObject(result);
            if(json==null){
                log.error("获取unionid 失败，accessToken:"+req.getAccessToken());
                return;
            }
            String unionid=json.getString("unionid");
            req.setUnionid(unionid);//设置 unionid
        }
        catch(Exception e){
            log.error("",e);
        }
    }

    /**
     * 获取unionUrl
     * @param accessToken
     * @return
     */
    private String getUnionUrl(String accessToken){
        //String url="https://graph.qq.com/oauth2.0/me?access_token=DGKFDJGJDF8346GFNF34BDF8DDF4&unionid=1";
        StringBuffer sb=new StringBuffer();
        sb.append("https://graph.qq.com/oauth2.0/me?access_token=").append(accessToken).append("&unionid=1");
        return sb.toString();
    }

    /**
     * 根据unionId获取用户
     */
    @Transactional(readOnly = true)
    public Player findByTencentOpenId(String openId) {
        List<Player> resultList = this.findByParams(Param.equal("tencentOpenid", openId));
        if (resultList == null || resultList.isEmpty()) {
            return null;
        }
        return resultList.get(0);
    }

    /**
     * 根据用户信息,请求QQ接口,获取用户QQ信息
     * @param channelId
     * @param accessToken
     * @param openid
     * @return
     */
    private ResTencentUserInfo getUserInfoNew(String channelId,String accessToken, String openid) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", accessToken);
        params.put("openid", openid);
        String appId=ChannelId.findByName(channelId).getAppId();
        params.put("oauth_consumer_key",appId);
        String result = HttpRequest.get("https://graph.qq.com/user/get_user_info", params, true).accept("application/json").body();
        return JSON.parseObject(result, ResTencentUserInfo.class);
    }

    public void loginExecute(Player player) {
        //同步player和playerdynamic数据
        playerDynamicService.synchdata(player);
        //更新登录时间
        playerLoginTimeService.updatePlayerLogin(player.getId());
        GameParams_Json cache= JsonCacheManager.getCache(GameParamsCache.class).getGameParams_Json();
        //判断是否为老用户
        //该处注释,此处问判断是否为老用户,如果为老用户,将对该用户发放内测奖励
//        if (player.getRegister() == Long.valueOf(cache.getDefaultRegisteredTime())) {
//            BagEntity bagEntity = bagService.load(player.getId());
//            if (!bagEntity.getBagItems().containsKey(cache.getBetaUserRewardId())) {
//                //添加奖励
//                bagEntity.getBagItems().put(cache.getBetaUserRewardId(), 1);
//                bagDao.save(bagEntity);
//                //解锁广场
//                playerTagsService.add(cache.getRewardTagId(), player);
//                //解锁标签
//                achievementService.addTag(player.getId(), cache.getRewardTagId());
//                systemNoticeService.sendSystemNotice(SystemNotice, 0, player.getId());
//            }
//        }
    }

    @Override
    public List<Player> listInitPlayer(int page) {
        int index = (page - 1 )* 500;
        Query query = em.createQuery("from Player where isGuest = 1");
        query.setFirstResult(index);//起始查找位置
        query.setMaxResults(500);//查询数量
        List<Player> list = query.getResultList();
        return list;
    }
}
