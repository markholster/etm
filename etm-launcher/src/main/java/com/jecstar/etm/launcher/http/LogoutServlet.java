package com.jecstar.etm.launcher.http;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LogoutServlet extends HttpServlet {

	/**
	 * The serialVersionID for this class.
	 */
	private static final long serialVersionUID = 2736840311198379670L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		HttpSession session = req.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		String source = req.getParameter("source");
		if (source != null) {
			resp.sendRedirect(source);
		}
	}
}
