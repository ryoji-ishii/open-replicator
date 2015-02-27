/* 
 * Copyright  (c) 2015 DMM.com Labo Co.,Ltd All Rights Reserved.
 */

package com.google.code.or.query;

import com.google.code.or.common.glossary.column.StringColumn;
import com.google.code.or.net.impl.packet.ResultSetFieldPacket;
import com.google.code.or.net.impl.packet.ResultSetRowPacket;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ryoji-ishii on 2015/02/06.
 */
public class QueryResultRow {
	/** */
	private List<String> columnNames;

	private Map<String, StringColumn> valueMap = new LinkedHashMap<String, StringColumn>();

	/**
	 * Constructor
	 * @param size column size
	 */
	QueryResultRow(int size) {
		this.columnNames = new ArrayList<String>(size);
	}

	/**
	 * Copy constructor
	 * @param row QueryResultRow instance
	 */
	QueryResultRow(QueryResultRow row) {
		this.columnNames = row.columnNames;
	}

	/**
	 *
	 * @param packet
	 */
	void fetch(ResultSetFieldPacket packet) {
		StringColumn column = packet.getOriginalColumn();
		String columnName = column != null ? column.toString() : "";
		if (columnName.length() <= 0) {
			column = packet.getColumn();
			columnName = column != null ? column.toString() : "";
		}
		this.columnNames.add(columnName);
	}

	/**
	 *
	 * @param packet
	 */
	void fetch(ResultSetRowPacket packet) {
		int size = this.columnNames.size();
		List<StringColumn> columns = packet.getColumns();
		String columnName;
		for (int i = 0; i < size; i++) {
			columnName = this.columnNames.get(i);
			this.valueMap.put(columnName, columns.get(i));
		}
	}

	public int size() {
		return this.columnNames.size();
	}

	public StringColumn getValue(int index) {
		String name = this.columnNames.get(index);
		return this.getValue(name);
	}

	public StringColumn getValue(String columnName) {
		return this.valueMap.get(columnName);
	}

	@Override
	public String toString() {
		StringBuilder buff = new StringBuilder();
		Iterator<Map.Entry<String, StringColumn>> entrySet = this.valueMap.entrySet().iterator();
		Map.Entry<String, StringColumn> entry = null;
		buff.append("{");
		while (entrySet.hasNext()) {
			if (entry != null) {
				buff.append(", ");
			}
			entry = entrySet.next();
			buff.append(entry.getKey()).append("=");
			buff.append(entry.getValue().toString());
		}
		buff.append("}");
		return buff.toString();
	}
}
