package com.github.corneil.tests;

import com.github.corneil.requirements.RequiresStaticSupport;

public class MyApp {
	public static void main(String[] args) {
		try {
			Class<?> implClass = Class.forName("com.github.corneil.tests.MyGoodImplementation");
			if (RequiresStaticSupport.checkClass(implClass)) {
				MyInterface impl = (MyInterface) (implClass.getMethod("create").invoke(null));
				assert impl != null;
			}
		} catch (Throwable x) {
			x.printStackTrace(System.err);
		}
		try {
			Class<?> implClass = Class.forName("com.github.corneil.tests.MyBadImplementation");
			if (RequiresStaticSupport.checkClass(implClass)) {
				MyInterface impl = (MyInterface) (implClass.getMethod("create").invoke(null));
				assert impl != null;
			}
		} catch (Throwable x) {
			x.printStackTrace(System.err);
		}
	}
}
