package com.jeffdisher.thinktank.auth;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.jeffdisher.breakwater.RestServer;
import com.jeffdisher.breakwater.StringMultiMap;


/**
 * Entry-points related to authentication:
 * -POST /login/&lt;UUID&gt;
 * -GET /getid
 * -POST /logout
 */
public class AuthEntryPoints {
	public static void registerEntryPoints(RestServer server) {
		// Install handlers for modifying login state.
		server.addPostHandler("/login", 1, (HttpServletRequest request, HttpServletResponse response, String[] pathVariables, StringMultiMap<String> formVariables, StringMultiMap<byte[]> multiPart, byte[] rawPost) -> {
			UUID uuid;
			try {
				uuid = UUID.fromString(pathVariables[0]);
			} catch (IllegalArgumentException e) {
				uuid = null;
			}
			if (null != uuid) {
				request.getSession().setAttribute("uuid", uuid);
				response.setContentType("text/plain;charset=utf-8");
				response.setStatus(HttpServletResponse.SC_OK);
				response.getWriter().println(uuid.toString());
			} else {
				request.getSession().invalidate();
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("Required UUID");
			}
		});
		server.addGetHandler("/getid", 0, (HttpServletRequest request, HttpServletResponse response, String[] variables) -> {
			HttpSession session = request.getSession(false);
			if (null != session) {
				response.setContentType("text/plain;charset=utf-8");
				response.setStatus(HttpServletResponse.SC_OK);
				response.getWriter().println(session.getAttribute("uuid").toString());
			} else {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("Not logged in");
			}
		});
		server.addPostHandler("/logout", 0, (HttpServletRequest request, HttpServletResponse response, String[] pathVariables, StringMultiMap<String> formVariables, StringMultiMap<byte[]> multiPart, byte[] rawPost) -> {
			request.getSession().invalidate();
			response.setContentType("text/plain;charset=utf-8");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println("logged out");
		});
	}
}
