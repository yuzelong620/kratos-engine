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
    private Logger loginLogger = LoggerFactory.getLogger("loginLogger");
    public static final short CMD_REQ_LOGIN = 1; // 请求微信登录

    @Autowired
    private GlobalData globalData;
    @Autowired
    private RoleIdService roleIdService;
    @Autowired
    private WechatManager wechatManager;
    @Autowired
    private TencentManager tencentManager;
    @Autowired
	private ScheduledService scheduledService;
    @Autowired
    UnionIdDao unionIdDao;
    @Autowired
    UnionIdService unionIdService;
//    @Autowired
//    UserDao userDao;
    @Autowired
    PlayerDynamicDao playerDynamicDao;
    @Autowired
    PlayerDynamicService playerDynamicService;

    @Autowired
    private BagService bagService;
    @Autowired
    private BagDao bagDao;
    @Autowired
    public static final String SystemNotice = "内测奖励发放通知:感谢您参加了内测版不眨眼，官方赠送您内测专属头像框，感谢支持！（请在我的荣誉里查收）";
    
    
    @Value("${spring.profiles.active}")
    private String profile;
   // @Lazy
   // @Autowired
   // private ChosenOptionService chosenOptionService;
    /**
     * 封号
     * @param playerId
     * @return
     */
    public boolean setIsBlock(long playerId){
    	Player player=get(playerId);
        player.setIsBlock(1);
        this.cacheAndPersist(playerId,player);
        GMDataChange.recordChange("通过玩家ID封号\t玩家ID为",playerId);
    	return true;
    }
    /**
     * @param playerId
     * @param time  禁言时间
     * @return
     */
    public boolean setNoSpeak(long playerId,long time){
    	Player player=get(playerId);
        player.setNoSpeakTime(time+System.currentTimeMillis());
        this.cacheAndPersist(playerId,player);
        GMDataChange.recordChange("禁言ID为" + playerId + "\t禁言时长(毫秒)",time);
    	return true;
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
 
    private ResTencentUserInfo getUserInfoNew(String channelId,String accessToken, String openid) {
        Map<String, String> params = new HashMap<>();
        params.put("access_token", accessToken);
        params.put("openid", openid);
        String appId=ChannelId.findByName(channelId).getAppId();
        params.put("oauth_consumer_key",appId);
        String result = HttpRequest.get("https://graph.qq.com/user/get_user_info", params, true).accept("application/json").body();
        return JSON.parseObject(result, ResTencentUserInfo.class);
    }

//    @Override 
    public ResPlayerProfile bindTencent2(ReqPlayerTencentLogin request) {
    	checkUnionidParams(request);
        Player player = PlayerContext.getPlayer();
        Player playerOld = findByTencentOpenId(request.getOpenId());
        if(playerOld != null){
            throw new BusinessException("该QQ已经绑定，请使用qq登录");
        }
        UnionIdEntity entity=unionIdDao.findByUnionId(request.getUnionid());//查找映射关系
        if(entity!=null){
        	 throw new BusinessException("该QQ已经绑定，请使用qq登录");
        }
        String openid=request.getOpenId();
        String accessToken=request.getAccessToken();
        
        player.setTencentOpenid(openid);
        ResTencentUserInfo userInfo =getUserInfoNew(request.getChannelId(),accessToken,openid);
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
       player.setAvatarUrl(userInfo.getFigureurl_qq());
       player.setIsGuest(0);
       playerDynamicService.synchdata(player);
       this.cacheAndPersist(player.getId(), player);
       unionIdService.save(request.getUnionid(),player.getId());//保存unionid 映射关系
       return new ResPlayerProfile(player);
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
             player.setAvatarUrl(userInfo.getFigureurl_qq());
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
    
    @Override
    public ResPlayerLogin tencentLogin(ReqPlayerTencentLogin request) {
        ResPlayerLogin resPlayerLogin = new ResPlayerLogin();
        String openId = request.getOpenId();
        Player player = findByTencentOpenId(openId);
        if (player == null) {
            player = this.tencentRegister(request.getAccessToken(), openId);
            resPlayerLogin.setCreate(true);
        }else if (player.getIsGuest() == 1) {
			player.setIsGuest(0);
			this.cacheAndPersist(player.getId(), player);	
        }
        playerDynamicService.synchdata(player);
        resPlayerLogin.setToken(player.getToken());
        resPlayerLogin.setPlayer(new ResPlayerProfile(player));
        return resPlayerLogin;
    }

    @Override
    public ResPlayerLogin guestLogin() {
        Player player = this.guestRegister();
        ResPlayerLogin resPlayerLogin = new ResPlayerLogin();
        resPlayerLogin.setCreate(true);
//        OnFire.fire(new PlayerLoginEvent(player));
        resPlayerLogin.setToken(player.getToken());
        resPlayerLogin.setPlayer(new ResPlayerProfile(player));
        return resPlayerLogin;
    }

    @Transactional(readOnly = true)
    public Player findByWechatOpenId(String openId) {
        List<Player> resultList = this.findByParams(Param.equal("wechatOpenid", openId));
        if (resultList == null || resultList.isEmpty()) {
            return null;
        }
        return resultList.get(0);
    }

    @Transactional(readOnly = true)
    public Player findByTencentOpenId(String openId) {
        List<Player> resultList = this.findByParams(Param.equal("tencentOpenid", openId));
        if (resultList == null || resultList.isEmpty()) {
            return null;
        }
        return resultList.get(0);
    }
   private String getUnionUrl(String accessToken){
//	   String url="https://graph.qq.com/oauth2.0/me?access_token=DGKFDJGJDF8346GFNF34BDF8DDF4&unionid=1"; 
		   StringBuffer sb=new StringBuffer();
		   sb.append("https://graph.qq.com/oauth2.0/me?access_token=").append(accessToken).append("&unionid=1");
		return sb.toString();
	   
   }

   
   @Autowired
   CommonService commonService;
   @Transactional(readOnly = true)
   public Player findByToken(String token) {
       Player player = JedisUtils.getInstance().get(token, Player.class);
       if(player == null) {
           List<Player> players = findByParams(Param.equal("token", token));
           if(players != null && !players.isEmpty()) {
               JedisUtils.getInstance().set(token, players.get(0));
               //重新设置  玩家属性加成
               player=players.get(0);
               PlayerDynamicEntity playerDynamic=playerDynamicService.load(player);
               //commonService.resetPlayerExtra(player, playerDynamic);
               return players.get(0);
           }
       }
       return player;
   }

    @Override
    public void cache(Long key, Player player) {
        super.cache(key, player);
        JedisUtils.getInstance().set(player.getToken(), player);
    }



    @Override
    public void index() {
        Player player = PlayerContext.getPlayer();
        player.setLastLoginTime(System.currentTimeMillis());
        loginLogger.info(player.getId() + ":" + player.getLastLoginTime());
        this.cacheAndPersist(player.getId(), player);
    }


    
    /**
     * 检查修正
     * @param player
     */
    public void checkAndAmendmentAchievementScore(Player player){
    	//获取player的所有 成就id
    	List<Integer> releasedAchievementIds = new ArrayList<>();
    	AchievementCache cache=JsonCacheManager.getCache( AchievementCache.class);
    	int sumAchievementScore=0;
        if (StringHelper.isNotBlank(player.getReleasedAchievements())) {
            String[] ids = player.getReleasedAchievements().split(",");
            for (String achievementId : ids) {
            	Integer id=Integer.parseInt(achievementId);
            	Achievement_Json json=cache.getData(id);
            	if(json==null){
            		log.error("修正成就数据，不存在的成就id:"+id);
            		continue;
            	}
                releasedAchievementIds.add(id);
                sumAchievementScore+=json.getAchievement();
            }
        }
        player.setAchievement(sumAchievementScore);
        player.setReleasedAchievements(StringHelper.join(releasedAchievementIds, ","));
    }


    @Override
    public ResPlayerProfile setPower(Integer power) {
        Player player = PlayerContext.getPlayer();
//        if (player.getPower() != null) {
//            return new ResPlayerProfile(player);
//        }
        player.setLastRecoverPowerTime(System.currentTimeMillis());
        player.setPower(100);
        this.cacheAndPersist(player.getId(), player);
        return new ResPlayerProfile(player);
    }




    @Override
    public ResPlayerProfile gmSetPower(String name, Integer power) {
        List<Player> players = findByParams(Param.equal("roleId", name));
        if (players != null && players.size() > 0) {
            Player player = players.get(0);
            player.setLastRecoverPowerTime(System.currentTimeMillis());
            player.setPower(power);
            this.cacheAndPersist(player.getId(), player);
            return new ResPlayerProfile(player);
        }
        return null;
    }


    @Override
    public ResPlayerProfile bindWechat(ReqPlayerWechatLogin request) {
        Player player = PlayerContext.getPlayer();
        ResAccessToken resAccessToken = wechatManager.getWechatService().getAppAccessToken(request.getCode());
        Player playerOld = findByWechatOpenId(resAccessToken.getOpenid());
        if(playerOld != null) {
           // remove(playerOld.getId());
        	throw new BusinessException("该微信已经绑定，请使用微信登录");
        }
        player.setIsGuest(0);
        this.setWechatInfo(player, resAccessToken.getAccess_token(), resAccessToken.getOpenid());
        playerDynamicService.synchdata(player);
        this.cacheAndPersist(player.getId(), player);
        return new ResPlayerProfile(player);
    }

    @Override
    public ResPlayerProfile bindTencent(ReqPlayerTencentLogin request) {
        Player player = PlayerContext.getPlayer();
        Player playerOld = findByTencentOpenId(request.getOpenId());
        if(playerOld != null) {
            //remove(playerOld.getId());
            throw new BusinessException("该QQ已经绑定，请使用QQ登录");
        }
        player.setIsGuest(0);
        this.setTencentInfo(player, request.getAccessToken(), request.getOpenId());
        playerDynamicService.synchdata(player);
        this.cacheAndPersist(player.getId(), player);
        return new ResPlayerProfile(player);
    }

    /** 结算时间 */
    static final long settleTime=1000L * 60 * 10;
    /** 最大体力 */
    static final int max_power=100;
    
    private void recoverPower(Player player, boolean persist) {
        Long lastTime = player.getLastRecoverPowerTime();
        long currentTime=System.currentTimeMillis();
        if (lastTime == null) {
            lastTime = currentTime;
        }
        int maxPower=player.getExtraPowerLimit()+max_power;
//        if(player.getPower()==null){//判空
//        	player.setPower(0);
//        }
        if(player.getPower()>=maxPower){//已经是上限不需要恢复
        	player.setLastRecoverPowerTime(currentTime);
        	if (persist) {
                this.cacheAndPersist(player.getId(), player);
            }
        	return;
        }
	    //恢复速度
	    long mySettleTime=settleTime;
	    if(player.getExtraRecoverRote()!=0){//如果有恢复加成，就减少恢复时间
	      	mySettleTime-=mySettleTime*player.getExtraRecoverRote();
	    }
	    //时间差
	    long sub=currentTime - lastTime;
	    if(sub<mySettleTime){
	        return;
	    }
	    int value=(int)(sub/mySettleTime)*10;
	    player.addPower(value);
	    long newTime=currentTime;
//	    if(player.getPower()<maxPower){//没有恢复满，等待下次恢复 
//	        newTime=mySettleTime*value+lastTime;
//	    }
	    player.setLastRecoverPowerTime(newTime);
        if (persist) {
	        this.cacheAndPersist(player.getId(), player);
	    }
        return;
    }
    
    public void recoverPower(Player player){
    	this.recoverPower(player,true);
    }
//     private void recoverPower(Player player, boolean persist) {
//        Long time = player.getLastRecoverPowerTime();
//        if (time == null) {
//            time = System.currentTimeMillis();
//        }
//        Long timeLeft = ((1000 * 60 * 4) - (System.currentTimeMillis() - time)) / 1000;
//
//        if (timeLeft <= 0) {
//            // 判断需要回复多少点电量
//            Long power = (System.currentTimeMillis() - time) / (1000 * 60 * 4);
//            player.addPower(power.intValue());
//            player.setLastRecoverPowerTime(System.currentTimeMillis());
//        }
//        if (persist) {
//            this.cacheAndPersist(player.getId(), player);
//        }
//    } 

    @Override
    public Player edit(ReqPlayerEdit reqPlayerEdit) {
        Player player = PlayerContext.getPlayer();
        if (StringHelper.isNoneBlank(reqPlayerEdit.getName())) {
            player.setName(reqPlayerEdit.getName());
        }
        if (StringHelper.isNoneBlank(reqPlayerEdit.getAvatarUrl())) {
            player.setAvatarUrl(reqPlayerEdit.getAvatarUrl());
        }
        if (StringHelper.isNoneBlank(reqPlayerEdit.getGender())) {
            player.setGender(reqPlayerEdit.getGender());
        }
        cacheAndPersist(player.getId(), player);
        return player;
    }

    @Override
    public ResPlayerProfile getProfile() {
        Player player = PlayerContext.getPlayer();
        if(StringHelper.isBlank(player.getRoleId())) {
            player.setRoleId(roleIdService.getNextRoleId());
            this.cacheAndPersist(player.getId(), player);
        }
        this.recoverPower(player, true);
        this.checkDailyReset(player);
        return new ResPlayerProfile(player);
    }


    @Override
    public void checkDailyReset(Player player) {
        long resetTimestamp = globalData.dailyResetTimestamp;
        if (player.getLastDailyReset() < resetTimestamp) {
            player.setLastDailyReset(globalData.dailyResetTimestamp);
            cache(player.getId(), player);
            onDailyReset(player);
        }
    }
    private void setTencentInfo(Player player, String accessToken, String openid) {
        player.setTencentOpenid(openid);
        ResTencentUserInfo userInfo = tencentManager.getTencentService().getUserInfo(accessToken, openid);
        player.setGender(userInfo.getGender().equals("男") ? "1" : "2");
        if (userInfo.getNickname() != null) {
            try {
                player.setNickName(new String(Base64.getEncoder().encode(userInfo.getNickname().getBytes()), CommonConstant.UTF8));
            } catch (UnsupportedEncodingException e) {
                log.error("", e);
            }
        }
        player.setAvatarUrl(userInfo.getFigureurl_qq());
    }

    private Player tencentRegister(String accessToken, String openid) {
        Player player = new Player();
        player.setRoleId(roleIdService.getNextRoleId());
        player.setIsGuest(0);
        this.setTencentInfo(player, accessToken, openid);
        return this.register(player);
    }

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

    private Player wechatRegister(String accessToken, String openid) {
        Player player = new Player();
        player.setRoleId(roleIdService.getNextRoleId());
        player.setIsGuest(0);
        this.setWechatInfo(player, accessToken, openid);
        return this.register(player);
    }

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
    
    public Player guestRegisterByPhone() {
        Player player = new Player();
        player.setRoleId(roleIdService.getNextRoleId());
        player.setIsGuest(0);
        try {
            player.setNickName(new String(Base64.getEncoder().encode(("guest" + player.getRoleId()).getBytes()), CommonConstant.UTF8));
        } catch (UnsupportedEncodingException e) {
            log.error("", e);
        }
        player.setGender("1");
        return this.register(player,100);
    }
    
    private Player register(Player player) {
    	return register(player,100);
    	
    }
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
    @Override
    public Player auth(ReqPlayerEdit request) {
        Player player = PlayerContext.getPlayer();
        if (request.getName() != null) {
            try {
                player.setNickName(new String(Base64.getEncoder().encode(request.getName().getBytes()), CommonConstant.UTF8));
            } catch (UnsupportedEncodingException e) {
                log.error("", e);
            }
        }
        player.setAvatarUrl(request.getAvatarUrl());
        player.setGender(request.getGender());
        cacheAndPersist(player.getId(), player);
        return player;
    }

    /**
     * 各个模块的业务日重置
     *
     * @param player 玩家
     */
    private void onDailyReset(Player player) {
        player.setCostedPower(0);
        player.setTakenCostPower20(false);
        player.setTakenCostPower100(false);
        player.setTakenShare(false);
        player.setSharedCount(0);
        player.setWatchAdTask(0);
        player.setTakenWatchAdTask(0);
        this.cacheAndPersist(player.getId(), player);
    }
    

	

		

//		List<UserEntity> arrUserEntity = userDao.findAllByPlayerIds(arrayplayerid);
//		for (UserEntity userEntity : arrUserEntity) {
//			for (PlayerAllBean playerAllBean : arrayplayerAllBean){
//				if(userEntity.getPlayerId()==Long.valueOf(playerAllBean.getPlayerid())){
//					playerAllBean.setPhone(userEntity.getPhone());
//				}
//			}
//
//		}
		

		
	
//		List<PlayerOnlineEntity> arrayPlayerOnlineEntity = playerOnlineDao.findByIdOnlineTime(arrayplayerid);
//		for (PlayerOnlineEntity playerOnlineEntity : arrayPlayerOnlineEntity) {
//			for (PlayerAllBean playerAllBean : arrayplayerAllBean) {
//				if(playerOnlineEntity.getPlayerId()==Long.valueOf(playerAllBean.getPlayerid())){
//					playerAllBean.setOnlineTime(playerOnlineEntity.getOnlineTime());
//				}
//			}
//		}
//
//
//		List<PlayerProperty> arrayisPlayercount = findisPlayerById(arrayplayerid);
//		for (PlayerProperty playerProperty : arrayisPlayercount) {
//			for (PlayerAllBean playerAllBean : arrayplayerAllBean) {
//					playerAllBean.setIsplayernum(playerProperty.getIsplayercount());
//			}
//		}
//		return arrayplayerAllBean;
//	}
		 

	public  void checkGameCatalog(){
		GameCatalogCache gameCatalogCache = JsonCacheManager.getCache(GameCatalogCache.class);
		CreatRoleCache creatRoleCache = JsonCacheManager.getCache(CreatRoleCache.class);
		 
		for(GameCatalog_Json json:gameCatalogCache.getList()){
			Integer playerId=json.getAuthorid();
			if(creatRoleCache.getData(playerId)==null){
				throw new RuntimeException("gamecatalog 表作者id 不存在！       id:"+json.getId()+",authorId:"+json.getAuthorid());
			}
		}
	}
	@Override
	public void creatRobot() {
		CreatRoleCache creatRoleCache = JsonCacheManager.getCache(CreatRoleCache.class);
		List<CreatRole_Json> creatRole_Jsons = creatRoleCache.getList();
		Player player = null;
		PlayerDynamicEntity playerDynamicEntity = null;
		long currentTime = System.currentTimeMillis();
		for (CreatRole_Json creatRole_Json : creatRole_Jsons) {
			player =get(Long.valueOf(creatRole_Json.getId()));
			if (player != null) {
				//修改机器人成就和探索度
				 player.setRoleId(creatRole_Json.getRoleId());
				 player.setExploration(creatRole_Json.getExploration());  
			     player.setAchievement(creatRole_Json.getAchievement());
			     player.setUpdate();
			     playerDynamicEntity = playerDynamicService.load(player);
			     playerDynamicEntity.setRoleId(creatRole_Json.getRoleId());
			     playerDynamicDao.save(playerDynamicEntity);
			     this.persist(player);
				continue;
			}
			player = new Player();
			player.setId(Long.valueOf(creatRole_Json.getId()));
			player.setRoleId(creatRole_Json.getRoleId());
			try {
	            player.setNickName(new String(Base64.getEncoder().encode(creatRole_Json.getNickName().getBytes()), CommonConstant.UTF8));
	        } catch (UnsupportedEncodingException e) {
	            log.error("", e);
	        }
			player.setIsGuest(0);
			player.setToken(UUID.randomUUID().toString());
			player.setAvatarUrl(creatRole_Json.getAvatarUrl());
			if ("男".equals(creatRole_Json.getGender())) {
				 player.setGender("1");
			}else {
				 player.setGender("2");
			}       
	        player.setLastRecoverPowerTime(currentTime);
	        player.setPower(100);
	        player.setExploration(creatRole_Json.getExploration());  
	        player.setAchievement(creatRole_Json.getAchievement());
	        playerDynamicEntity = new PlayerDynamicEntity(player);
	        this.persist(player);
	        playerDynamicDao.save(playerDynamicEntity);
		}
	}	

	
	@Override
	public Long getPlayerByRoleId(String roleId) {
		Query query = em.createNativeQuery("SELECT ID FROM PLAYER WHERE ROLE_ID=?");
		query.setParameter(1, roleId);
		List<Object> resultList = query.getResultList();
		if (resultList == null||resultList.size() == 0) {
			return null;
		}
		return Long.valueOf(resultList.get(0).toString());
	}

	
	/**
	 * 查询数据库违规总数
	 * */
	@Override
	@Deprecated
	public long badcount() {
		Query query = em.createNativeQuery("SELECT COUNT(*) FROM player WHERE is_block = 1 OR no_speak_time !=0 ");
		Player player = new Player();
		List<Object> resultList = query.getResultList();	
		int num = 0;
		for (Object obj : resultList) {
			num = Integer.valueOf(obj.toString());
		}
		JedisUtils ju = JedisUtils.getInstance();
		ju.set("playerNum", num);
		return num;
	}


	public void loginExecute(Player player) {
		//同步player和playerdynamic数据
		  playerDynamicService.synchdata(player);
		  GameParams_Json cache=JsonCacheManager.getCache(GameParamsCache.class).getGameParams_Json();
		  //判断是否为老用户
		  if (player.getRegister() == Long.valueOf(cache.getDefaultRegisteredTime())) {
			  BagEntity bagEntity = bagService.load(player.getId());
			  if (!bagEntity.getBagItems().containsKey(cache.getBetaUserRewardId())) {
				  //添加奖励
				  bagEntity.getBagItems().put(cache.getBetaUserRewardId(), 1);
				  bagDao.save(bagEntity);

			  }
		}
	}

}
