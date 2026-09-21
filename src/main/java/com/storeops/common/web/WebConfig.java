package com.storeops.common.web;

import com.storeops.common.auth.ActorArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Wires shared web concerns: today only caller resolution. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final ActorArgumentResolver actorArgumentResolver;

  public WebConfig(ActorArgumentResolver actorArgumentResolver) {
    this.actorArgumentResolver = actorArgumentResolver;
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(actorArgumentResolver);
  }
}
