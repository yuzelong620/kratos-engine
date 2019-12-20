package com.kratos.engine.framework.db;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractCacheable extends Cacheable {
	
	/** 是否已经持久化 */
	private AtomicBoolean persistent = new AtomicBoolean(false);
	
	@Override
	public DbStatus getStatus() {
		return this.status;
	}

	@Override
	public final boolean isInsert() {
		return this.status == DbStatus.INSERT;
	}

	@Override
	public final boolean isUpdate() {
		return this.status == DbStatus.UPDATE;
	}

	@Override
	public final boolean isDelete() {
		return this.status == DbStatus.DELETE;
	}

	@Override
	public void setInsert() {
		this.status = DbStatus.INSERT;
	}

	@Override
	public final void setUpdate(){
		//只有该状态才可以变更为update
		if (this.status == DbStatus.NORMAL) {
			this.status = DbStatus.UPDATE;
		}
	}

	@Override
	public final void setDelete(){
		if (this.status == DbStatus.INSERT) {
			this.status = DbStatus.NORMAL;
		} else{
			this.status = DbStatus.DELETE;
		}
	}
	
	public final void resetDbStatus() {
		this.status = DbStatus.NORMAL;
	}
	
	/**
	 * 	标记为已经持久化
	 */
	public void markPersistent() {
		persistent.compareAndSet(false, true);
	}

	/**
	 * 是否数据库已有对应的实体
	 * @return
	 */
	public boolean existedInDb() {
		return persistent.get();
	}
	
	public void autoSetStatus() {
		// 删除状态只能手动设置
		if (!isDelete()) {
			// 如果已经存在于数据库，则表示修改记录
			if (existedInDb()) {
				setUpdate();
			} else {
				setInsert();
			}
		}
	}
}
