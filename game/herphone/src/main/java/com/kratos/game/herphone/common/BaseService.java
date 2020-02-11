package com.kratos.game.herphone.common;

import com.kratos.game.herphone.bag.dao.BagDao;
import com.kratos.game.herphone.blackList.dao.BlackListDao;
import com.kratos.game.herphone.message.dao.MessageDao;
import com.kratos.game.herphone.message.dao.MessageFirstDao;
import com.kratos.game.herphone.player.dao.ExchangeRewardDao;
import com.kratos.game.herphone.player.dao.RewardDao;
import com.kratos.game.herphone.player.service.PlayerService;
import com.kratos.game.herphone.player.service.PlayerServiceImpl;
import com.kratos.game.herphone.playerDynamic.dao.PlayerDynamicDao;
import com.kratos.game.herphone.playerOnline.dao.PlayerLoginTimeDao;
import com.kratos.game.herphone.playerOnline.dao.PlayerOnlineDao;
import com.kratos.game.herphone.playerOnline.service.PlayerLoginTimeService;
import com.kratos.game.herphone.sms.dao.SmsDao;
import com.kratos.game.herphone.systemMessgae.dao.PublicSystemMessageDao;
import com.kratos.game.herphone.systemMessgae.dao.SystemMessageLastDao;
import com.kratos.game.herphone.systemMessgae.dao.SystemMessgaeDao;
import com.kratos.game.herphone.tencent.dao.UnionIdDao;
import com.kratos.game.herphone.user.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BaseService extends BaseController{
//	@Autowired
//	protected SystemService systemService;
	@Autowired
	protected ExchangeRewardDao ExchangeRewardDao;
	@Autowired
	protected CommonService commonService;
	@Autowired
	protected BagDao bagDao;
	@Autowired
	protected SmsDao smsDao;
	@Autowired
	protected PlayerDynamicDao playerDynamicDao;
	@Autowired
	protected UnionIdDao unionIdDao;
	@Autowired
	protected RewardDao RewardDao;
	@Autowired
	protected SystemMessgaeDao systemMessgaeDao;
	@Autowired
	protected SystemMessageLastDao systemMessageLastDao;
	@Autowired
	protected PublicSystemMessageDao publicSystemMessageDao;
	@Autowired
	protected PlayerService playerService;
	@Autowired
	protected PlayerServiceImpl playerServiceImpl;
	@Autowired
	protected PlayerLoginTimeDao playerLoginTimeDao;
	@Autowired
	protected UserDao userDao;
	@Autowired
	protected PlayerLoginTimeService playerLoginTimeService;
	@Autowired
	protected PlayerOnlineDao playerOnlineDao;
	@Autowired
	protected MessageDao messageDao;
	@Autowired
	protected MessageFirstDao messageFirstDao;
	@Autowired
	protected BlackListDao blackListDao;
}
