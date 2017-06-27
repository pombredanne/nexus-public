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
package org.sonatype.nexus.orient.transaction;

import org.sonatype.nexus.common.property.SystemPropertiesHelper;
import org.sonatype.nexus.common.sequence.NumberSequence;
import org.sonatype.nexus.common.sequence.RandomExponentialSequence;
import org.sonatype.nexus.transaction.Transaction;
import org.sonatype.nexus.transaction.UnitOfWork;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.tx.OTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link Transaction} backed by an OrientDB connection.
 *
 * @since 3.0
 */
public class OrientTransaction
    implements Transaction
{
  private static final Logger log = LoggerFactory.getLogger(OrientTransaction.class);

  private static final int INITIAL_DELAY_MS = SystemPropertiesHelper
      .getInteger(OrientTransaction.class.getName() + ".retrydelay.initial", 10);

  private static final int MAX_RETRIES = 8;

  private final ODatabaseDocumentTx db;

  private int retries = 0;

  private NumberSequence retryDelay;

  OrientTransaction(final ODatabaseDocumentTx db) {
    this.db = checkNotNull(db);
  }

  public ODatabaseDocumentTx getDb() {
    return db;
  }

  /**
   * @return current OrientDB connection.
   *
   * @throws IllegalArgumentException if no connection exists in the current context
   */
  public static ODatabaseDocumentTx currentDb() {
    final Transaction tx = UnitOfWork.currentTx();
    if (tx instanceof OrientTransaction) {
      return ((OrientTransaction) tx).db;
    }
    try {
      // support alternative formats which just need to provide a 'getDb' method
      return (ODatabaseDocumentTx) tx.getClass().getMethod("getDb").invoke(tx);
    }
    catch (final Exception e) {
      throw new IllegalArgumentException("Transaction " + tx + " has no public 'getDb' method", e);
    }
  }

  @Override
  public void begin() {
    db.begin();
  }

  @Override
  public void commit() {
    db.commit();
    retries = 0;
  }

  @Override
  public void rollback() {
    db.rollback();
  }

  @Override
  public void close() {
    db.close();
  }

  @Override
  public boolean isActive() {
    OTransaction tx = null;
    if (db.isActiveOnCurrentThread()) {
      tx = db.getTransaction();
    }
    return tx != null && tx.isActive();
  }

  @Override
  public boolean allowRetry(final Exception cause) {
    if (retries < MAX_RETRIES) {
      try {
        if (retryDelay == null) {
          retryDelay = delaySequence();
        }
        long delay = retryDelay.next();
        log.trace("Delaying tx retry for {}ms", delay);
        Thread.sleep(delay);
      }
      catch (final InterruptedException e) {
        throw new RuntimeException(e);
      }
      retries++;
      log.debug("Retrying operation: {}/{}", retries, MAX_RETRIES);
      return true;
    }
    log.warn("Reached max retries: {}/{}", retries, MAX_RETRIES);
    return false;
  }

  private static NumberSequence delaySequence() {
    return RandomExponentialSequence.builder()
        .start(INITIAL_DELAY_MS) // start at 10ms
        .factor(2) // delay an average of 100% longer, each time
        .maxDeviation(.5) // ±50%
        .build();
  }
}
