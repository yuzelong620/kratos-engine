package com.kratos.engine.framework.scene.ai;

import com.kratos.engine.framework.scene.actor.Creature;

public interface State {
	
	/**
	 * 切换至新状态
	 * @param creature
	 */
	void onEnter(Creature creature);
	
	/**
	 * 离开当前状态
	 * @param creature
	 */
	void onExit(Creature creature);
	
	/**
	 * 每一个tick跑的业务
	 * @param creature
	 */
	void execute(Creature creature);

}
