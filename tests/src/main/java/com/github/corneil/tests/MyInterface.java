package com.github.corneil.tests;

import com.github.corneil.requirements.RequiresStatic;

@RequiresStatic(MyTemplate.class)
public interface MyInterface {
	void doSomeThing();
	String getSomething();
}
