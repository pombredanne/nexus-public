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
package org.sonatype.nexus.common.thread

import java.security.SecureClassLoader

import org.sonatype.goodies.testsupport.TestSupport

import org.junit.Test

/**
 * Tests for {@link TcclBlock}.
 */
class TcclBlockTest
    extends TestSupport
{
  @Test
  void 'begin and restore class-loader'() {
    Thread thread = Thread.currentThread()
    ClassLoader original = thread.contextClassLoader
    ClassLoader classLoader = new SecureClassLoader(getClass().classLoader) {}

    def tccl = TcclBlock.begin(classLoader)
    try {
      assert thread.contextClassLoader.is(classLoader)
    }
    finally {
      tccl.close()
    }
    assert thread.contextClassLoader.is(original)
  }
}
