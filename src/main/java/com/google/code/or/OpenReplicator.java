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
package com.google.code.or;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.code.or.binlog.*;
import com.google.code.or.binlog.impl.ReplicationBasedBinlogParser;
import com.google.code.or.binlog.impl.parser.DeleteRowsEventParser;
import com.google.code.or.binlog.impl.parser.DeleteRowsEventV2Parser;
import com.google.code.or.binlog.impl.parser.FormatDescriptionEventParser;
import com.google.code.or.binlog.impl.parser.GtidEventParser;
import com.google.code.or.binlog.impl.parser.IncidentEventParser;
import com.google.code.or.binlog.impl.parser.IntvarEventParser;
import com.google.code.or.binlog.impl.parser.QueryEventParser;
import com.google.code.or.binlog.impl.parser.RandEventParser;
import com.google.code.or.binlog.impl.parser.RotateEventParser;
import com.google.code.or.binlog.impl.parser.StopEventParser;
import com.google.code.or.binlog.impl.parser.TableMapEventParser;
import com.google.code.or.binlog.impl.parser.UpdateRowsEventParser;
import com.google.code.or.binlog.impl.parser.UpdateRowsEventV2Parser;
import com.google.code.or.binlog.impl.parser.UserVarEventParser;
import com.google.code.or.binlog.impl.parser.WriteRowsEventParser;
import com.google.code.or.binlog.impl.parser.WriteRowsEventV2Parser;
import com.google.code.or.binlog.impl.parser.XidEventParser;
import com.google.code.or.common.glossary.column.StringColumn;
import com.google.code.or.common.util.MySQLConstants;
import com.google.code.or.net.impl.packet.command.ComBinlogDumpGtidPacket;
import com.google.code.or.query.QueryHelper;
import com.google.code.or.io.impl.SocketFactoryImpl;
import com.google.code.or.net.Packet;
import com.google.code.or.net.Transport;
import com.google.code.or.net.TransportException;
import com.google.code.or.net.impl.AuthenticatorImpl;
import com.google.code.or.net.impl.TransportImpl;
import com.google.code.or.net.impl.packet.ErrorPacket;
import com.google.code.or.net.impl.packet.command.ComBinlogDumpPacket;
import com.google.code.or.query.QueryResultRow;
import com.google.code.or.query.QueryResultSet;

/**
 * 
 * @author Jingqi Xu
 */
public class OpenReplicator {
	//
	protected int port = 3306;
	protected String host;
	protected String user;
	protected String password;
	protected int serverId = 6789;
	protected String binlogFileName;
	protected long binlogPosition = 4;
	protected String encoding = "utf-8";
	protected int level1BufferSize = 1024 * 1024;
	protected int level2BufferSize = 8 * 1024 * 1024;
	protected int socketReceiveBufferSize = 512 * 1024;

	protected boolean checksumEnabled;
	protected boolean verifyChecksum;
	protected boolean gtidEnabled;

	//
	protected Transport transport;
	protected BinlogParser binlogParser;
	protected BinlogEventListener binlogEventListener;
	protected BinlogParserListener binlogParserListener;
	protected final AtomicBoolean running = new AtomicBoolean(false);
	
	/**
	 * 
	 */
	public boolean isRunning() {
		return this.running.get();
	}

	public void start(BinlogStartHandler startHandler, BinlogStopHandler stopHandler, BinlogErrorHandler errorHandler) throws Exception {
		this.start(new BinlogParserListenerImpl(startHandler, stopHandler, errorHandler));
	}

	public void start() throws Exception {
		this.start(null);
	}
	
	public void start(BinlogParserListener parserListener) throws Exception {
		//
		if(!this.running.compareAndSet(false, true)) {
			return;
		}
		if (parserListener != null) {
			this.binlogParserListener = parserListener;
		}
		//
		if(this.transport == null) this.transport = getDefaultTransport();
		this.transport.connect(this.host, this.port);

		this.checksumEnabled = this.isEnabledChecksum();
		if (this.checksumEnabled) {
			this.useChecksum();
		}
		if (this.binlogFileName == null) {
			this.adjustPosition();
		}

		//
		if (this.gtidEnabled) {
			dumpBinGtidlog();
		} else {
			dumpBinlog();
		}
		//
		if(this.binlogParser == null) this.binlogParser = getDefaultBinlogParser();
		this.binlogParser.setEventListener(this.binlogEventListener);
		this.binlogParser.addParserListener(this.binlogParserListener);
		this.binlogParser.start();
	}

	public void stop(long timeout, TimeUnit unit) throws Exception {
		//
		if(!this.running.compareAndSet(true, false)) {
			return;
		}
		
		//
		this.transport.disconnect(this.checksumEnabled);
		this.binlogParser.stop(timeout, unit);
	}
	
	public void stopQuietly(long timeout, TimeUnit unit) {
		try {
			stop(timeout, unit);
		} catch(Exception e) {
			// NOP
		}
	}
	
	/**
	 * 
	 */
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	
	public int getServerId() {
		return serverId;
	}

	public void setServerId(int serverId) {
		this.serverId = serverId;
	}
	
	public long getBinlogPosition() {
		return binlogPosition;
	}

	public void setBinlogPosition(long binlogPosition) {
		this.binlogPosition = binlogPosition;
	}
	
	public String getBinlogFileName() {
		return binlogFileName;
	}

	public void setBinlogFileName(String binlogFileName) {
		this.binlogFileName = binlogFileName;
	}
	
	public int getLevel1BufferSize() {
		return level1BufferSize;
	}

	public void setLevel1BufferSize(int level1BufferSize) {
		this.level1BufferSize = level1BufferSize;
	}

	public int getLevel2BufferSize() {
		return level2BufferSize;
	}

	public void setLevel2BufferSize(int level2BufferSize) {
		this.level2BufferSize = level2BufferSize;
	}
	
	public int getSocketReceiveBufferSize() {
		return socketReceiveBufferSize;
	}

	public void setSocketReceiveBufferSize(int socketReceiveBufferSize) {
		this.socketReceiveBufferSize = socketReceiveBufferSize;
	}

	public boolean isVerifyChecksum() {
		return verifyChecksum;
	}

	public void setVerifyChecksum(boolean verifyChecksum) {
		this.verifyChecksum = verifyChecksum;
	}

	public boolean isGtidEnabled() {
		return gtidEnabled;
	}

	public void setGtidEnabled(boolean gtidEnabled) {
		this.gtidEnabled = gtidEnabled;
	}

	/**
	 * 
	 */
	public Transport getTransport() {
		return transport;
	}

	public void setTransport(Transport transport) {
		this.transport = transport;
	}
	
	public BinlogParser getBinlogParser() {
		return binlogParser;
	}

	public void setBinlogParser(BinlogParser parser) {
		this.binlogParser = parser;
	}
	
	public BinlogEventListener getBinlogEventListener() {
		return binlogEventListener;
	}

	public void setBinlogEventListener(BinlogEventListener listener) {
		this.binlogEventListener = listener;
	}

	/**
	 *
	 * @return
	 * @throws Exception
	 */
	protected boolean isEnabledChecksum() throws Exception {
		QueryResultSet resultSet = QueryHelper.query(this.transport, "SHOW GLOBAL VARIABLES LIKE 'BINLOG_CHECKSUM'");
		while (resultSet.hasNext()) {
			QueryResultRow row = resultSet.next();
			String checksum = row.getValue("VARIABLE_VALUE").toString();
			if (!"NONE".equals(checksum)) {
				return true;
			}
		}
		return false;
	}

	/**
	 *
	 * @throws Exception
	 */
	protected void useChecksum() throws Exception {
		int cnt = QueryHelper.execute(this.transport, "set @master_binlog_checksum= @@global.binlog_checksum");
	}

	/**
	 *
	 * @return
	 * @throws Exception
	 */
	protected long adjustPosition() throws Exception {
		QueryResultSet resultSet = QueryHelper.query(this.transport, "SHOW MASTER STATUS");
		while (resultSet.hasNext()) {
			QueryResultRow row = resultSet.next();
			this.binlogFileName = row.getValue("File").toString();
			this.binlogPosition = Long.parseLong(row.getValue("Position").toString());
		}
		return this.binlogPosition;
	}

	/**
	 * 
	 */
	protected void dumpBinlog() throws Exception {
		//
		final ComBinlogDumpPacket command = new ComBinlogDumpPacket();
		command.setBinlogFlag(0);
		command.setServerId(this.serverId);
		command.setBinlogPosition(this.binlogPosition);
		command.setBinlogFileName(StringColumn.valueOf(this.binlogFileName.getBytes(this.encoding)));
		this.transport.getOutputStream().writePacket(command);
		this.transport.getOutputStream().flush();
		
		//
		final Packet packet = this.transport.getInputStream().readPacket();
		if(packet.getPacketBody()[0] == ErrorPacket.PACKET_MARKER) {
			final ErrorPacket error = ErrorPacket.valueOf(packet);
			throw new TransportException(error);
		} 
	}

	protected void dumpBinGtidlog() throws Exception {
		//
		final ComBinlogDumpGtidPacket command = new ComBinlogDumpGtidPacket();
		command.setBinlogFlag(MySQLConstants.BINLOG_THROUGH_POSITION);
		command.setServerId(this.serverId);
		command.setBinlogPosition(this.binlogPosition);
		command.setBinlogFileName(StringColumn.valueOf(this.binlogFileName.getBytes(this.encoding)));
		this.transport.getOutputStream().writePacket(command);
		this.transport.getOutputStream().flush();

		//
		final Packet packet = this.transport.getInputStream().readPacket();
		if(packet.getPacketBody()[0] == ErrorPacket.PACKET_MARKER) {
			final ErrorPacket error = ErrorPacket.valueOf(packet);
			throw new TransportException(error);
		}
	}
	
	protected Transport getDefaultTransport() throws Exception {
		//
		final TransportImpl r = new TransportImpl();
		r.setLevel1BufferSize(this.level1BufferSize);
		r.setLevel2BufferSize(this.level2BufferSize);
		
		//
		final AuthenticatorImpl authenticator = new AuthenticatorImpl();
		authenticator.setUser(this.user);
		authenticator.setPassword(this.password);
		authenticator.setEncoding(this.encoding);
		r.setAuthenticator(authenticator);
		
		//
		final SocketFactoryImpl socketFactory = new SocketFactoryImpl();
		socketFactory.setKeepAlive(true);
		socketFactory.setTcpNoDelay(false);
		socketFactory.setReceiveBufferSize(this.socketReceiveBufferSize);
		r.setSocketFactory(socketFactory);
		return r;
	}
	
	protected ReplicationBasedBinlogParser getDefaultBinlogParser() throws Exception {
		//
		final ReplicationBasedBinlogParser r = new ReplicationBasedBinlogParser();
		r.registgerEventParser(new StopEventParser());
		r.registgerEventParser(new RotateEventParser());
		r.registgerEventParser(new IntvarEventParser());
		r.registgerEventParser(new XidEventParser());
		r.registgerEventParser(new RandEventParser());
		r.registgerEventParser(new QueryEventParser());
		r.registgerEventParser(new UserVarEventParser());
		r.registgerEventParser(new IncidentEventParser());
		r.registgerEventParser(new TableMapEventParser());
		r.registgerEventParser(new WriteRowsEventParser());
		r.registgerEventParser(new UpdateRowsEventParser());
		r.registgerEventParser(new DeleteRowsEventParser());
		r.registgerEventParser(new WriteRowsEventV2Parser());
		r.registgerEventParser(new UpdateRowsEventV2Parser());
		r.registgerEventParser(new DeleteRowsEventV2Parser());
		r.registgerEventParser(new FormatDescriptionEventParser());
		r.registgerEventParser(new GtidEventParser());
		
		//
		r.setTransport(this.transport);
		r.setBinlogFileName(this.binlogFileName);
		r.setChecksumEnabled(this.checksumEnabled);
		r.setVerifyChecksum(this.verifyChecksum);
		return r;
	}

	private class BinlogParserListenerImpl implements BinlogParserListener {
		protected BinlogStartHandler binlogStartHandler;
		protected BinlogErrorHandler binlogErrorHandler;
		protected BinlogStopHandler binlogStopHandler;

		private BinlogParserListenerImpl(BinlogStartHandler startHandler, BinlogStopHandler stopHandler, BinlogErrorHandler errorHandler) {
			this.binlogStartHandler = startHandler;
			this.binlogStopHandler = stopHandler;
			this.binlogErrorHandler = errorHandler;
		}

		@Override
		public void onStart(BinlogParser parser) {
			if (this.binlogStartHandler != null) {
				this.binlogStartHandler.onStart();
			}
		}

		@Override
		public void onStop(BinlogParser parser) {
			if (this.binlogStopHandler != null) {
				this.binlogStopHandler.onStop();
			}
		}

		@Override
		public void onException(BinlogParser parser, Exception exception) {
			if (this.binlogErrorHandler != null) {
				this.binlogErrorHandler.onError(exception);
			}
		}
	}
}
