package com.github.corneil.tests;

public class MyGoodImplementation implements MyInterface {
	@Override
	public void doSomeThing() {

	}

	@Override
	public String getSomething() {
		return "something";
	}

	public static MyInterface create() {
		return new MyGoodImplementation();
	}
}
