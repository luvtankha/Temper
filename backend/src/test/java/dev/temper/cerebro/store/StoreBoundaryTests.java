package dev.temper.cerebro.store;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.mock.web.*;

class StoreBoundaryTests {
    @Test void legacyEndpointsAreNotExposedByStoreDeployment()throws Exception{var filter=new StoreBoundaryFilter();for(String path:new String[]{"/api/messages","/api/overlay/analyze","/ws","/actuator/env"}){var request=new MockHttpServletRequest("GET",path);var response=new MockHttpServletResponse();filter.doFilter(request,response,new MockFilterChain());assertEquals(404,response.getStatus());}}
    @Test void boundedInputRejectsEvenUnspecifiedLength()throws Exception{var request=new MockHttpServletRequest("POST","/api/store/verify");request.setContent(new byte[16385]);var response=new MockHttpServletResponse();new StoreBoundaryFilter().doFilter(request,response,new MockFilterChain());assertEquals(413,response.getStatus());}
    @Test void validBoundaryKeepsBodyForController()throws Exception{var request=new MockHttpServletRequest("POST","/api/store/verify");request.setContent("{}".getBytes());var response=new MockHttpServletResponse();var chain=new MockFilterChain();new StoreBoundaryFilter().doFilter(request,response,chain);assertNotNull(chain.getRequest());assertEquals("{}",new String(chain.getRequest().getInputStream().readAllBytes()));assertEquals("no-store",response.getHeader("Cache-Control"));}
}
