package com.jecstar.etm.launcher;

import com.jecstar.etm.launcher.http.BCrypt;

public class PasswordGenerator {

	public static void main(String[] args) {
		System.out.println(BCrypt.hashpw("test", BCrypt.gensalt()));
	}
}
