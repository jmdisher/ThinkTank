package com.jeffdisher.thinktank.utilities;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.Arrays;

import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.util.resource.ResourceCollection;

import com.jeffdisher.breakwater.utilities.Assert;


/**
 * Helper routines for creating Jetty Resource objects from paths.
 */
public class ResourceHelpers {
	/**
	 * Converts an array of string representations of paths to a ResourceCollection of PathResources.
	 * Note that this creates "real paths" so that Jetty doesn't think the paths are aliases.
	 * 
	 * @param stringPaths The paths to convert.
	 * @return The ResourceCollection containing the PathResources of the "real paths" of the given strings.
	 * @throws IOException If any path is invalid.
	 */
	public static ResourceCollection buildResourceCollection(String... stringPaths) throws IOException {
		if (null == stringPaths) {
			throw new NullPointerException("Paths must not be null");
		}
		if (0 == stringPaths.length) {
			throw new IllegalArgumentException("Must provide at least one path");
		}
		PathResource[] paths = Arrays.stream(stringPaths).map((String stringPath) -> _buildPathResource(stringPath)).toArray(PathResource[]::new);
		return new ResourceCollection(paths);
	}

	/**
	 * Converts a string representation of a path to a PathResource.
	 * Note that this creates a "real path" so that Jetty doesn't think the path is an alias.
	 * 
	 * @param path The path to convert.
	 * @return The PathResource representing the "real path".
	 * @throws IOException If the path is invalid.
	 */
	public static PathResource buildPathResource(String path) throws IOException {
		return _buildPathResource(path);
	}


	private static PathResource _buildPathResource(String path) {
		// NOTE:  We MUST create this as a "real path" or Jetty decides it is an alias and doesn't follow it when loading resources.
		PathResource pathResource;
		try {
			pathResource = new PathResource(Paths.get(path).toRealPath(new LinkOption[0]));
		} catch (IOException e1) {
			// We treat a failure to resolve this path as a fatal, and highly expected, error.
			throw Assert.unexpected(e1);
		}
		return pathResource;
	}
}
