package com.google.code.or.binlog.impl.event;

import com.google.code.or.common.util.MySQLConstants;
import com.google.code.or.common.util.ToStringBuilder;

import java.util.Arrays;

public class GtidEvent extends AbstractBinlogEventV4
{
  public static final int EVENT_TYPE = MySQLConstants.GTID_LOG_EVENT;

  private static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
  									'9', 'a', 'b', 'c', 'd', 'e', 'f'};

  private final byte[] sourceId;
  private final long transactionId;

  public GtidEvent(byte[] sourceId, long transactionId) {
    this.sourceId = sourceId;
    this.transactionId = transactionId;
  }

  private String toUUIDString(byte[] bytes) {
    if (bytes.length == 16) {
      StringBuilder buff = new StringBuilder(36);
        for (int i = 0; i < 16; i++) {
          buff.append(DIGITS[0x0f & (bytes[i] >> 4)]).append(DIGITS[0x0f & (bytes[i])]);
          switch (i) {
          case 3:
          case 5:
          case 7:
          case 9:
            buff.append("-");
          }
		}
        return buff.toString();
	}
	return Arrays.toString(bytes);
  }

  /**
   *
   */
  @Override
  public String toString() {
	  return new ToStringBuilder(this)
          .append("header", header)
          .append("sourceId", this.toUUIDString(this.sourceId))
          .append("transactionId", transactionId).toString();
  }

  public byte[] getSourceId() {
    return sourceId;
  }

  public long getTransactionId() {
    return transactionId;
  }
}
