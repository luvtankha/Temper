package dev.temper.cerebro.learning;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component @ConditionalOnProperty(name="temper.learning.enabled",havingValue="true")
public final class LearningBoundaryFilter extends OncePerRequestFilter {
    private final Semaphore slots=new Semaphore(4);
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        response.setHeader("Cache-Control","no-store");response.setHeader("X-Content-Type-Options","nosniff");
        if("/actuator/health".equals(request.getRequestURI())&&"GET".equals(request.getMethod())){chain.doFilter(request,response);return;}
        boolean submit="POST".equals(request.getMethod())&&"/api/learning/sessions".equals(request.getRequestURI()),delete="DELETE".equals(request.getMethod())&&"/api/learning/contributions".equals(request.getRequestURI());
        if(!submit&&!delete){response.setStatus(404);return;}if(request.getContentLengthLong()>384_000){response.setStatus(413);return;}
        String auth=request.getHeader("Authorization");if(auth==null||!auth.matches("Bearer [A-Za-z0-9_-]{43}")){response.setStatus(401);return;}
        if(!slots.tryAcquire()){response.setStatus(429);return;}
        try{byte[] body=request.getInputStream().readNBytes(384_001);if(body.length>384_000){response.setStatus(413);return;}HttpServletRequestWrapper bounded=new HttpServletRequestWrapper(request){@Override public ServletInputStream getInputStream(){ByteArrayInputStream input=new ByteArrayInputStream(body);return new ServletInputStream(){public int read(){return input.read();}public boolean isFinished(){return input.available()==0;}public boolean isReady(){return true;}public void setReadListener(ReadListener listener){throw new UnsupportedOperationException();}};}@Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8));}};chain.doFilter(bounded,response);}finally{slots.release();}
    }
}
