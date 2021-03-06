package com.appengine.frame.filters;

import com.appengine.frame.utils.RequestLogRecord;
import com.appengine.frame.utils.ResponseWrapper;
import com.appengine.frame.context.RequestContext;
import com.appengine.frame.context.ThreadLocalContext;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RequestLogFilter extends OncePerRequestFilter {

    protected static final Logger logger = LoggerFactory.getLogger("REQUEST");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (StringUtils.startsWith(path, "/webjars") || StringUtils.startsWith(path, "/static")) {
            filterChain.doFilter(request, response);
            return;
        }

        RequestContext context = ThreadLocalContext.getRequestContext();

        response = new ResponseWrapper(response);
        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long endTime = System.currentTimeMillis();
            RequestLogRecord record = new RequestLogRecord();
            record.setRequestId(context.getRequestId());
            record.setIp(request.getRemoteHost());
            record.setUseTime(endTime - startTime);
            record.setApi(request.getRequestURI());
            record.setMethod(request.getMethod());
            record.setParameters(request.getParameterMap());
            record.setResponseStatus(response.getStatus());
            record.setResponse(new String(((ResponseWrapper) response).toByteArray(), response.getCharacterEncoding()));
            //text/html不打印body
            if (!StringUtils.contains(response.getContentType(), "application/json")) {
                record.setWriteBody(false);
            }
            MDC.put("CUSTOM_LOG", "request");
            logger.info(record.toString());
            MDC.remove("CUSTOM_LOG");
            ThreadLocalContext.clear();
        }
    }

    private boolean isMultipart(final HttpServletRequest request) {
        return request.getContentType() != null && request.getContentType().startsWith("multipart/form-data");
    }
}
