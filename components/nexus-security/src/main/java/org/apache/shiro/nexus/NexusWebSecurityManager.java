/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.apache.shiro.nexus;

import javax.cache.CacheManager;
import javax.inject.Inject;
import javax.inject.Provider;

import org.sonatype.nexus.common.event.EventManager;
import org.sonatype.nexus.security.UserIdMdcHelper;
import org.sonatype.nexus.security.authc.AuthenticationEvent;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.apache.shiro.web.mgt.WebSecurityManager;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Custom {@link WebSecurityManager}.
 *
 * @since 2.7.2
 */
public class NexusWebSecurityManager
    extends DefaultWebSecurityManager
{
  private final Provider<EventManager> eventManager;

  @Inject
  public NexusWebSecurityManager(final Provider<EventManager> eventManager,
                                 final Provider<CacheManager> cacheManager)
  {
    this.eventManager = checkNotNull(eventManager);
    setCacheManager(new ShiroJCacheManagerAdapter(cacheManager));
    //explicitly disable rememberMe
    this.setRememberMeManager(null); 
  }

  /**
   * Post {@link AuthenticationEvent}.
   */
  private void post(final AuthenticationToken token, final boolean successful) {
    eventManager.get().post(new AuthenticationEvent(token.getPrincipal().toString(), successful));
  }

  /**
   * After login set the userId MDC attribute.
   */
  @Override
  public Subject login(Subject subject, final AuthenticationToken token) {
    try {
      subject = super.login(subject, token);
      UserIdMdcHelper.set(subject);
      post(token, true);
      return subject;
    }
    catch (AuthenticationException e) {
      post(token, false);
      throw e;
    }
  }

  /**
   * After logout unset the userId MDC attribute.
   */
  @Override
  public void logout(final Subject subject) {
    super.logout(subject);
    UserIdMdcHelper.unset();
  }
}
