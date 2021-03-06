package com.patex.opds.latest;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

/**
 * Created by Alexey on 09.04.2017.
 */
@Aspect
@Component
public class LatestURIAspect {

    @Autowired
    private LatestURIComponent latestURIComponent;

    @AfterReturning(pointcut = "@annotation(com.patex.opds.latest.SaveLatest)", returning = "view")
    public void afterMethod(ModelAndView view) {
        latestURIComponent.afterMethod(view);
    }
}
