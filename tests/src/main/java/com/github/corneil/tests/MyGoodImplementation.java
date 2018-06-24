package com.github.corneil.tests;

public class MyGoodImplementation implements MyInterface {
	public static MyInterface create() {
		return new MyGoodImplementation();
	}

	@Override
	public void doSomeThing() {

	}

	@Override
	public String getSomething() {
		return "something";
	}
}
