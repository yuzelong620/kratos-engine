package com.kratos.game.herphone.common;

import com.kratos.game.herphone.bag.service.BagService;
import com.kratos.game.herphone.gamemanager.service.AdminUserService;
import com.kratos.game.herphone.gamemanager.service.GameScoreService;
import com.kratos.game.herphone.player.service.PlayerService;
import com.kratos.game.herphone.playerDynamic.service.PlayerDynamicService;
import com.kratos.game.herphone.sms.service.SmsService;
import com.kratos.game.herphone.systemMessgae.service.PublicSystemMessageService;
import com.kratos.game.herphone.systemMessgae.service.SystemMessageLastService;
import com.kratos.game.herphone.systemMessgae.service.SystemMessgaeService;
import com.kratos.game.herphone.tencent.service.UnionIdService;
import org.springframework.beans.factory.annotation.Autowired;



public abstract class BaseController {
	@Autowired
    protected BagService bagService;
	@Autowired
	protected SmsService smsService;
	@Autowired
	protected PlayerDynamicService playerDynamicService;
	@Autowired
	protected UnionIdService unionIdService;
	@Autowired
	protected SystemMessgaeService systemMessgaeService;
	@Autowired
	protected AdminUserService adminUserService;
	@Autowired
	protected SystemMessageLastService systemMessageLastService;
	@Autowired
	protected GameScoreService gameScoreService ;

	@Autowired
	protected PublicSystemMessageService publicSystemMessageService;

	@Autowired
	protected PlayerService playerService;

//	@Autowired
//	protected SystemService systemService;


}
