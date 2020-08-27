package com.jeffdisher.thinktank.utilities;

import java.io.File;

import org.eclipse.jetty.util.resource.Resource;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


public class ResourceHelpersTest {
	@Rule
	public TemporaryFolder _folder = new TemporaryFolder();

	@Test
	public void testBasicPaths() throws Throwable {
		File folder1 = _folder.newFolder();
		File folder2 = _folder.newFolder();
		String file1 = "file1";
		String file2 = "file2";
		boolean didCreate = new File(folder1, file1).createNewFile();
		Assert.assertTrue(didCreate);
		didCreate = new File(folder2, file2).createNewFile();
		Assert.assertTrue(didCreate);
		
		Resource resource = ResourceHelpers.buildPathResource(folder1.getAbsolutePath());
		Assert.assertNotNull(resource.getResource(file1));
		resource = ResourceHelpers.buildResourceCollection(folder1.getAbsolutePath(), folder2.getAbsolutePath());
		Assert.assertNotNull(resource.getResource(file1));
		Assert.assertNotNull(resource.getResource(file2));
	}
}
