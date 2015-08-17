/* 
 * Copyright  (c) 2015 DMM.com Labo Co.,Ltd All Rights Reserved.
 */

package com.google.code.or;

import com.google.code.or.common.util.MySQLUtils;

import java.util.Arrays;

/**
 * Created by ryoji-ishii on 2015/03/11.
 */
public class ScrambleTest {

	public static void main(String[] args) {
		String password = "or_test";
		String seed1 = "ht0/=\"3v";
		String seed2 = "}/qnT8Z?hotx";
		//String seed = seed1 + seed2;
		String seed = "e#}n2J^zj&zY.w.|KADo\u0000";

		byte[] scrambleSeed = seed.getBytes();
		byte[] array = MySQLUtils.password41OrLater(password.getBytes(), scrambleSeed);
		System.out.println(Arrays.toString(array));
	}

}
