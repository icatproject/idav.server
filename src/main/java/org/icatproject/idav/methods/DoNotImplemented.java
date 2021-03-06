package org.icatproject.idav.methods;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.icatproject.idav.IMethodExecutor;
import org.icatproject.idav.WebdavStatus;

public class DoNotImplemented implements IMethodExecutor {

    private static org.slf4j.Logger LOG = org.slf4j.LoggerFactory
            .getLogger(DoNotImplemented.class);
    private boolean _readOnly;

    public DoNotImplemented(boolean readOnly) {
        _readOnly = readOnly;
    }

    public void execute(String authString, HttpServletRequest req,
            HttpServletResponse resp) throws IOException {
        LOG.trace("-- " + req.getMethod());

        if (_readOnly) {
            resp.sendError(WebdavStatus.SC_FORBIDDEN);
        } else
            resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
    }
}
