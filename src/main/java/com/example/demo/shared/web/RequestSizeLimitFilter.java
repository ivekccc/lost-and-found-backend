package com.example.demo.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;

import java.io.IOException;
@Component
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final long maxRequestBytes;
    private final MessageSource messageSource;
    private final LocaleResolver localeResolver;

    public RequestSizeLimitFilter(@Value("${app.security.max-request-bytes}") long maxRequestBytes,
                                  MessageSource messageSource, LocaleResolver localeResolver) {
        this.maxRequestBytes = maxRequestBytes;
        this.messageSource = messageSource;
        this.localeResolver = localeResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > maxRequestBytes) {
            reject(request, response);
            return;
        }
        filterChain.doFilter(new LimitedRequest(request, maxRequestBytes), response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String message = messageSource.getMessage("error.requestTooLarge", null, localeResolver.resolveLocale(request));
        ErrorResponseWriter.write(response, HttpStatus.CONTENT_TOO_LARGE, message);
    }

    private static final class LimitedRequest extends HttpServletRequestWrapper {

        private final long limit;
        private ServletInputStream stream;

        private LimitedRequest(HttpServletRequest request, long limit) {
            super(request);
            this.limit = limit;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (stream == null) {
                stream = new LimitedInputStream(super.getInputStream(), limit);
            }
            return stream;
        }
    }

    private static final class LimitedInputStream extends ServletInputStream {

        private final ServletInputStream delegate;
        private final long limit;
        private long read;

        private LimitedInputStream(ServletInputStream delegate, long limit) {
            this.delegate = delegate;
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value != -1) {
                count(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int count = delegate.read(buffer, offset, length);
            if (count > 0) {
                count(count);
            }
            return count;
        }

        private void count(long bytes) throws IOException {
            read += bytes;
            if (read > limit) {
                throw new RequestBodyTooLargeException(limit);
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            delegate.setReadListener(listener);
        }
    }
}
