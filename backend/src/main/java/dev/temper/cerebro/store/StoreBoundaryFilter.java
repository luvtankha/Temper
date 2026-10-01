package dev.temper.cerebro.store;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** The store deployment exposes no legacy messaging/model endpoints and bounds untrusted input. */
@Component
@ConditionalOnProperty(name="temper.store.enabled",havingValue="true")
public final class StoreBoundaryFilter extends OncePerRequestFilter {
    private final Semaphore slots=new Semaphore(4);
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        response.setHeader("Cache-Control","no-store");response.setHeader("X-Content-Type-Options","nosniff");
        if("/actuator/health".equals(request.getRequestURI())&&"GET".equals(request.getMethod())){chain.doFilter(request,response);return;}
        if(!"/api/store/verify".equals(request.getRequestURI())||!"POST".equals(request.getMethod())){response.setStatus(404);return;}
        if(request.getContentLengthLong()>16384){response.setStatus(413);return;}
        if(!slots.tryAcquire()){response.setStatus(429);return;}
        try{
            byte[] body=request.getInputStream().readNBytes(16385);if(body.length>16384){response.setStatus(413);return;}
            HttpServletRequestWrapper bounded=new HttpServletRequestWrapper(request){
                @Override public ServletInputStream getInputStream(){ByteArrayInputStream input=new ByteArrayInputStream(body);return new ServletInputStream(){public int read(){return input.read();}public boolean isFinished(){return input.available()==0;}public boolean isReady(){return true;}public void setReadListener(ReadListener listener){throw new UnsupportedOperationException();}};}
                @Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8));}
            };
            chain.doFilter(bounded,response);
        }finally{slots.release();}
    }
}
