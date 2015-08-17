/* 
 * Copyright  (c) 2015 DMM.com Labo Co.,Ltd All Rights Reserved.
 */

package com.google.code.or.binlog;

/**
 *
 * Created by ishii-ryoji on 2015/08/03.
 */
public interface BinlogErrorHandler {

	void onError(Throwable e);

}
