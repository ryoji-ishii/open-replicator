/* 
 * Copyright  (c) 2015 DMM.com Labo Co.,Ltd All Rights Reserved.
 */

package com.google.code.or.query;

import com.google.code.or.common.glossary.column.StringColumn;
import com.google.code.or.net.Packet;
import com.google.code.or.net.Transport;
import com.google.code.or.net.TransportException;
import com.google.code.or.net.impl.packet.ErrorPacket;
import com.google.code.or.net.impl.packet.ResultSetHeaderPacket;
import com.google.code.or.net.impl.packet.command.ComQuery;
import com.google.code.or.query.QueryResultSet;

/**
 * Created by ryoji-ishii on 2015/02/06.
 */
public class QueryHelper {

	public static QueryResultSet query(Transport transport, String query) throws Exception {
		final ComQuery command = new ComQuery();
		command.setSql(StringColumn.valueOf(query.getBytes()));
		transport.getOutputStream().writePacket(command);
		transport.getOutputStream().flush();

		Packet packet = transport.getInputStream().readPacket();
		if(packet.getPacketBody()[0] == ErrorPacket.PACKET_MARKER) {
			final ErrorPacket error = ErrorPacket.valueOf(packet);
			throw new TransportException(error);
		}
		final ResultSetHeaderPacket header = ResultSetHeaderPacket.valueOf(packet);
		QueryResultSet resultSet = new QueryResultSet(header);
		resultSet.fetch(transport);
		return resultSet;
	}

	public static int execute(Transport transport, String query) throws Exception {
		final ComQuery command = new ComQuery();
		command.setSql(StringColumn.valueOf(query.getBytes()));
		transport.getOutputStream().writePacket(command);
		transport.getOutputStream().flush();

		Packet packet = transport.getInputStream().readPacket();
		if(packet.getPacketBody()[0] == ErrorPacket.PACKET_MARKER) {
			final ErrorPacket error = ErrorPacket.valueOf(packet);
			throw new TransportException(error);
		}
		final ResultSetHeaderPacket header = ResultSetHeaderPacket.valueOf(packet);
		return header.getFieldCount().intValue();
	}

}
