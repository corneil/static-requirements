package com.github.corneil.tests;

public class MyBadImplementation implements MyInterface {
	@Override
	public void doSomeThing() {
		// only illustrative
	}

	@Override
	public String getSomething() {
		return "something";
	}
}
