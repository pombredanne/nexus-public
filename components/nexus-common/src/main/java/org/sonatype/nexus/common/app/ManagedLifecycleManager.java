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
package org.sonatype.nexus.common.app;

import org.sonatype.nexus.common.app.ManagedLifecycle.Phase;

/**
 * Manages {@link ManagedLifecycle} components.
 *
 * @since 3.3
 */
public interface ManagedLifecycleManager
{
  /**
   * Returns the current phase.
   */
  Phase getCurrentPhase();

  /**
   * Attempts to move to the target phase by starting (or stopping) components phase-by-phase. If any components have
   * appeared since the last request which belong to the current phase or earlier then they are automatically started
   * before the current phase is changed. Similarly components that have disappeared are stopped.
   */
  void to(Phase targetPhase) throws Exception;
}
