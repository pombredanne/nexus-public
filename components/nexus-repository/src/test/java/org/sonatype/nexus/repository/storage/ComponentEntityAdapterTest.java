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
package org.sonatype.nexus.repository.storage;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.sonatype.goodies.testsupport.TestSupport;
import org.sonatype.nexus.common.collect.NestedAttributesMap;
import org.sonatype.nexus.common.entity.EntityHelper;
import org.sonatype.nexus.orient.testsupport.DatabaseInstanceRule;

import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.sonatype.nexus.repository.storage.MetadataNodeEntityAdapter.P_ATTRIBUTES;

public class ComponentEntityAdapterTest
    extends TestSupport
{
  private final static String GROUP = "group";

  @Rule
  public DatabaseInstanceRule database = DatabaseInstanceRule.inMemory("test");

  private ComponentEntityAdapter entityAdapter;

  private Bucket bucket;

  @Before
  public void setUp() {
    BucketEntityAdapter bucketEntityAdapter = new BucketEntityAdapter();
    entityAdapter = new ComponentEntityAdapter(bucketEntityAdapter);
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      bucketEntityAdapter.register(db);
      bucket = new Bucket();
      bucket.attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
      bucket.setRepositoryName("test-repo");
      bucketEntityAdapter.addEntity(db, bucket);
    }
  }

  @Test
  public void testRegister() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      entityAdapter.register(db);
      OSchema schema = db.getMetadata().getSchema();
      assertThat(schema.getClass(entityAdapter.getTypeName()), is(notNullValue()));
    }
  }

  @Test
  public void testBrowseByQuery_SpecialCharacters() {
    String special = ",;.:-_#'+*~<>|!\"§$%&/()=?{\\}";
    String group = "g-" + special;
    String name = "n-" + special;
    String version = "v-" + special;

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      entityAdapter.register(db);

      Component component = createComponent(group, name, version);
      entityAdapter.addEntity(db, component);

      Query query = Query.builder().where("group").eq(group).and("name").eq(name).and("version").eq(version).build();
      List<Component> components = Lists.newArrayList(entityAdapter.browseByQuery(db, query.getWhere(),
          query.getParameters(), Collections.singleton(bucket), query.getQuerySuffix()));

      assertThat(components, hasSize(1));
      assertThat(components.get(0).group(), is(group));
      assertThat(components.get(0).name(), is(name));
      assertThat(components.get(0).version(), is(version));
    }
  }

  @Test
  public void testBrowseByNameCaseInsensitive() {
    String name = "CamelCase";
    String version = "version";

    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      entityAdapter.register(db);

      Component component = createComponent(GROUP, name, version);
      entityAdapter.addEntity(db, component);

      List<Component> components1 =
          Lists.newArrayList(entityAdapter.browseByNameCaseInsensitive(db, "camelcase",
              Collections.singleton(bucket), null));

      assertThat(components1, hasSize(1));
      assertThat(components1.get(0).name(), is(name));
      assertThat(components1.get(0).version(), is(version));

      List<Component> components2 =
          Lists.newArrayList(entityAdapter.browseByNameCaseInsensitive(db, "CAMELCASE",
              Collections.singleton(bucket), null));

      assertThat(components2, hasSize(1));
      assertThat(components2.get(0).name(), is(name));
      assertThat(components2.get(0).version(), is(version));

    }
  }

  @Test
  public void testBrowseByNameCaseInsensitiveWithLimit() {
    try (ODatabaseDocumentTx db = database.getInstance().connect()) {
      entityAdapter.register(db);

      Component component1 = createComponent(GROUP, "name", "version1");
      entityAdapter.addEntity(db, component1);

      Component component2 = createComponent(GROUP, "name", "version2");
      entityAdapter.addEntity(db, component2);

      List<Component> allComponents =
          Lists.newArrayList(entityAdapter.browseByNameCaseInsensitive(db, "name",
              Collections.singleton(bucket), "limit 2"));

      assertThat(allComponents, hasSize(2));

      List<Component> firstComponent =
          Lists.newArrayList(entityAdapter.browseByNameCaseInsensitive(db, "name",
              Collections.singleton(bucket), "limit 1"));

      assertThat(firstComponent, hasSize(1));
    }
  }

  private Component createComponent(final String group, final String name, final String version) {
    Component component = new Component();
    component.bucketId(EntityHelper.id(bucket));
    component.format("format-id");
    component.attributes(new NestedAttributesMap(P_ATTRIBUTES, new HashMap<>()));
    component.group(group).name(name).version(version);
    return component;
  }
}
