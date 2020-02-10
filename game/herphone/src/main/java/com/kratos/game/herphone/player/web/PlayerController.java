package com.kratos.game.herphone.player.web;

import java.util.List;
import java.util.Map;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.globalgame.auto.json.Achievement_Json;
import com.kratos.engine.framework.net.socket.exception.BusinessException;
import com.kratos.game.herphone.aop.PrePermissions;
import com.kratos.game.herphone.common.BaseController;
//import com.kratos.game.herphone.auto.server.AchievementConfig;
import com.kratos.game.herphone.player.domain.Player;
import com.kratos.game.herphone.player.message.ReqPlayerEdit;
import com.kratos.game.herphone.player.message.ReqPlayerTencentLogin;
import com.kratos.game.herphone.player.message.ReqPlayerWechatLogin;
import com.kratos.game.herphone.player.message.ResPlayerLogin;
import com.kratos.game.herphone.player.message.ResPlayerProfile;
import com.kratos.game.herphone.player.message.ResRankPlayer;
import com.kratos.game.herphone.sms.message.ReqSmsCode;

import com.globalgame.auto.json.GameParams_Json;
import com.kratos.game.herphone.json.JsonCacheManager;
import com.kratos.game.herphone.json.datacache.GameParamsCache;
@RestController
@RequestMapping("/player")
@PrePermissions
public class PlayerController extends BaseController{

    /**
     * 游客登陆
     */
    @PostMapping("/guest/login")
    @PrePermissions(required = false)
    public ResponseEntity<ResPlayerLogin> guestLogin() {
        return new ResponseEntity<>(playerService.guestLogin(), HttpStatus.OK);
    }

    /**
     * qq登录
     */

}
