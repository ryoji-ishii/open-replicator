/* 
 * Copyright  (c) 2015 DMM.com Labo Co.,Ltd All Rights Reserved.
 */

package com.google.code.or.binlog.impl.variable.status;

import com.google.code.or.common.util.MySQLConstants;
import com.google.code.or.common.util.ToStringBuilder;
import com.google.code.or.io.XInputStream;

import java.io.IOException;

/**
 * Created by ryoji-ishii on 2015/02/09.
 */
public class QCommitTs extends AbstractStatusVariable {
	//
	public static final int TYPE = MySQLConstants.Q_COMMIT_TS;

	private long commitSeqNo;

	/**
	 */
	public QCommitTs(long commitSeqNo) {
		super(TYPE);
		this.commitSeqNo = commitSeqNo;
	}

	public long getCommitSeqNo() {
		return commitSeqNo;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("commitSeqNo", commitSeqNo).toString();
	}

	public static QCommitTs valueOf(XInputStream tis) throws IOException {
		return new QCommitTs(tis.readLong(8));
	}
}
