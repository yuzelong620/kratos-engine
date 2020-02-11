package com.kratos.game.herphone.user.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import com.kratos.engine.framework.net.socket.exception.BusinessException;
import com.kratos.game.herphone.common.BaseService;
import com.kratos.game.herphone.player.PlayerContext;
import com.kratos.game.herphone.player.domain.Player;
import com.kratos.game.herphone.player.message.ResPlayerLogin;
import com.kratos.game.herphone.player.message.ResPlayerProfile;
import com.kratos.game.herphone.player.web.GMDataChange;
import com.kratos.game.herphone.playerDynamic.entity.PlayerDynamicEntity;
import com.kratos.game.herphone.user.entity.UserEntity;

@Component
public class UserService extends BaseService{
	//手机登录
	public ResPlayerLogin validationBinding(String phone){
		UserEntity userEntity = userDao.findByID(phone);
		if (userEntity == null ) {
			throw new BusinessException("用户不存在,请前往注册");
		}
		long playerId = userEntity.getPlayerId();
		Player player = playerServiceImpl.get(playerId);
		if (player.getIsGuest() == 1) {
			player.setIsGuest(0);
			playerServiceImpl.cacheAndPersist(playerId, player);
		}
		playerServiceImpl.loginExecute(player);
		ResPlayerLogin resPlayerLogin = new ResPlayerLogin();
		resPlayerLogin.setCreate(false);
//	    OnFire.fire(new PlayerLoginEvent(player));
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

	//手机注册用户
	public ResPlayerLogin registerByPhone(String phone){
		UserEntity userEntity = userDao.findByID(phone);

		if (userEntity != null ) {
			throw new BusinessException("用户已存在,请直接登录");
		}
		Player player = playerServiceImpl.guestRegisterByPhone();
		ResPlayerLogin resPlayerLogin = new ResPlayerLogin();
		resPlayerLogin.setCreate(true);
//	    OnFire.fire(new PlayerLoginEvent(player));
		resPlayerLogin.setToken(player.getToken());
		resPlayerLogin.setPlayer(new ResPlayerProfile(player));
		long timeStemp = System.currentTimeMillis();
		userEntity = new UserEntity(player.getId(), phone, timeStemp);
		userDao.save(userEntity);
		playerDynamicService.synchdata(player);
		playerLoginTimeService.updatePlayerLogin(player.getId());
		return resPlayerLogin;
	}
}
