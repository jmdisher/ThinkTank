package com.jeffdisher.thinktank.utilities;

import org.junit.Assert;
import org.junit.Test;


public class MainHelpersTest {
	@Test
	public void testArgumentParsing() throws Throwable {
		String value = MainHelpers.getArgument(new String[] {"--adf", "--test", "value"}, "test");
		Assert.assertEquals("value", value);
		value = MainHelpers.getArgument(new String[] {"--adf", "-t", "value"}, "test");
		Assert.assertEquals("value", value);
		value = MainHelpers.getArgument(new String[] {"--adf", "--test", "value"}, "not");
		Assert.assertEquals(null, value);
		value = MainHelpers.getArgument(new String[] {"--adf", "--test", "value", "--test", "value2"}, "test");
		Assert.assertEquals("value", value);
		
		boolean flag = MainHelpers.getFlag(new String[] {"--adf", "--test", "value"}, "test");
		Assert.assertTrue(flag);
		flag = MainHelpers.getFlag(new String[] {"--adf", "--test", "value"}, "not");
		Assert.assertFalse(flag);
	}
}
