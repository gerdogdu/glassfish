/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package test;

import java.io.IOException;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AsyncListenerTimeoutServlet extends HttpServlet implements AsyncListener {
    private static StringBuffer sb = new StringBuffer();

    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {

        if ("1".equals(req.getParameter("result"))) {
            res.getWriter().println(sb.toString());
            sb.delete(0, sb.length());
            return;
        }

        if (!req.isAsyncSupported()) {
            throw new ServletException("Async not supported when it should");
        }

        AsyncContext ac = null;
        boolean isWrap = Boolean.parseBoolean(req.getParameter("wrap"));
        if (isWrap) {
            ac = req.startAsync(req, res);
        } else {
            ac = req.startAsync();
        }

        ac.addListener(this);
    }

    public void onComplete(AsyncEvent event) throws IOException {
        // do nothing
    }

    public void onTimeout(AsyncEvent event) throws IOException {
        AsyncContext ac = event.getAsyncContext();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        System.out.println("classLoader = " + cl);
        try {
            ac.getResponse().getWriter().println(cl.loadClass("test.MyObject").newInstance());
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
        ac.getRequest().setAttribute("a", "A");
        ac.complete();
        sb.append(ac.getRequest().getAttribute("a"));
    }

    public void onError(AsyncEvent event) throws IOException {
        // do nothing
    }

    public void onStartAsync(AsyncEvent event) throws IOException {
        // do nothing
    }
}
