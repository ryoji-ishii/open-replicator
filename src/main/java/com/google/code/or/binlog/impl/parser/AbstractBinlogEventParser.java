/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.code.or.binlog.impl.parser;

import com.google.code.or.binlog.BinlogEventParser;
import com.google.code.or.binlog.BinlogEventV4Header;
import com.google.code.or.common.util.MySQLConstants;
import com.google.code.or.io.XInputStream;

import java.io.IOException;

/**
 * 
 * @author Jingqi Xu
 */
public abstract class AbstractBinlogEventParser implements BinlogEventParser {
	//
	protected final int eventType;
	
	/**
	 * 
	 */
	public AbstractBinlogEventParser(int eventType) {
		this.eventType = eventType;
	}

	/**
	 * 
	 */
	public final int getEventType() {
		return eventType;
	}

	/**
	 *
	 * @param is
	 * @param header
	 * @param verifyChecksum
	 * @throws IOException
	 */
	public void validateChecksum(XInputStream is, BinlogEventV4Header header, boolean verifyChecksum) throws IOException {
		long checksum = is.readLong(MySQLConstants.BINLOG_CHECKSUM_LEN);
		int length = (int)header.getEventLength() - MySQLConstants.BINLOG_CHECKSUM_LEN;
		if (verifyChecksum && !is.validate(checksum, length)) {
			throw new IOException("detect replication data corruption.");
		}
	}
}
