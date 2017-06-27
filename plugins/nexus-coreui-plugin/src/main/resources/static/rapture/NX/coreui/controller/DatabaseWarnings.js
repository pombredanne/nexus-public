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
/*global Ext*/

/**
 * Database freeze warning controller, handles showing messages.
 *
 * @since 3.2
 */
Ext.define('NX.coreui.controller.DatabaseWarnings', {
  extend: 'NX.app.Controller',

  requires: [
    'NX.I18n',
    'NX.Permissions'
  ],
  refs: [
    {
      ref: 'databaseFreezeWarning',
      selector: '#nx-database-freeze-warning'
    }
  ],

  /**
   * @override
   */
  init: function() {

    var me = this;

    me.listen({
      controller: {
        '#State': {
          changed: me.stateChanged
        }
      }
    });
  },

  stateChanged: function() {
    var me = this,
        warningPanel = me.getDatabaseFreezeWarning(),
        databaseFreezeState = NX.State.getValue('db', {})['dbFrozen'],
        quorumState = NX.State.getValue('quorum', {})['quorumPresent'];

    if (warningPanel) {
      if (!quorumState) {
        warningPanel.setTitle(NX.I18n.get('Nodes_Quorum_lost_warning'));
      }

      // Read-only mode will take precedence and be the only message shown.
      if (databaseFreezeState) {
        warningPanel.setTitle(NX.I18n.get('Nodes_Read_only_mode_warning'));
      }

      if (!quorumState || databaseFreezeState) {
        warningPanel.show();
      }
      else {
        warningPanel.hide();
      }
    }
  }
});
