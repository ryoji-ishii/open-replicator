/* 
 * Copyright  (c) 2015 DMM.com Labo Co.,Ltd All Rights Reserved.
 */

package com.google.code.or.query;

import com.google.code.or.net.Packet;
import com.google.code.or.net.Transport;
import com.google.code.or.net.impl.packet.EOFPacket;
import com.google.code.or.net.impl.packet.ResultSetFieldPacket;
import com.google.code.or.net.impl.packet.ResultSetHeaderPacket;
import com.google.code.or.net.impl.packet.ResultSetRowPacket;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by ryoji-ishii on 2015/02/06.
 */
public class QueryResultSet implements Iterator<QueryResultRow> {
	/** */
	private int fieldCount;
	/** */
	private List<QueryResultRow> rowList = new LinkedList<QueryResultRow>();
	/** */
	private int currentIndex;

	/**
	 * Constructor
	 * @param header
	 */
	QueryResultSet(ResultSetHeaderPacket header) {
		this.fieldCount = header.getFieldCount().intValue();
	}

	int fetch(Transport transport) throws IOException {
		if (this.fieldCount <= 0) {
			return 0;
		}
		Packet packet;
		QueryResultRow row = new QueryResultRow(this.fieldCount);
		while(true) {
			packet = transport.getInputStream().readPacket();
			if(packet.getPacketBody()[0] == EOFPacket.PACKET_MARKER) {
				EOFPacket eof = EOFPacket.valueOf(packet);
				break;
			} else {
				ResultSetFieldPacket field = ResultSetFieldPacket.valueOf(packet);
				row.fetch(field);
			}
		}
		QueryResultRow prev = null;
		while(true) {
			if (prev != null) {
				row = new QueryResultRow(prev);
			}
			packet = transport.getInputStream().readPacket();
			if(packet.getPacketBody()[0] == EOFPacket.PACKET_MARKER) {
				EOFPacket eof = EOFPacket.valueOf(packet);
				break;
			} else {
				row.fetch(ResultSetRowPacket.valueOf(packet));
				this.rowList.add(row);
			}
			prev = row;
		}
		return this.rowList.size();
	}

	@Override
	public boolean hasNext() {
		return this.currentIndex < this.rowList.size();
	}

	@Override
	public QueryResultRow next() {
		return this.rowList.get(this.currentIndex++);
	}

	@Override
	public void remove() {
		this.rowList.remove(--this.currentIndex);
	}
}
