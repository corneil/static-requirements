package com.github.corneil.requirements;

import org.junit.Assert;
import org.junit.Test;

public class RequiresStaticTest {
	public static class StaticTemplate {
		public static InterfaceRequiresStatic create(String arg) {
			throw new NoSuchMethodError("create");
		}
	}

	@RequiresStatic(StaticTemplate.class)
	public interface InterfaceRequiresStatic {
		String someMethod();

		void otherMethod(String arg);
	}

	public static class GoodAttempt implements InterfaceRequiresStatic {
		@Override
		public String someMethod() {
			return null;
		}

		@Override
		public void otherMethod(String arg) {
		}

		public static InterfaceRequiresStatic create(String arg) {
			InterfaceRequiresStatic result = new GoodAttempt();
			result.otherMethod(arg);
			return result;
		}
	}

	public static class BadAttempt implements InterfaceRequiresStatic {
		@Override
		public String someMethod() {
			return null;
		}

		@Override
		public void otherMethod(String arg) {

		}
	}

	@Test
	public void testGood() throws RequiresStaticException {
		Assert.assertTrue(RequiresStaticSupport.checkClass(GoodAttempt.class));
	}

	@Test(expected = RequiresStaticException.class)
	public void testBad() throws RequiresStaticException {
		try {
			Assert.assertFalse(RequiresStaticSupport.checkClass(BadAttempt.class));
		} catch (RequiresStaticException x) {
			System.err.println("Expected:" + x.toString());
			throw x;
		}
	}

}
