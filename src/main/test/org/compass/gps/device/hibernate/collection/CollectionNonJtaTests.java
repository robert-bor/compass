/*
 * Copyright 2004-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.compass.gps.device.hibernate.collection;

import junit.framework.TestCase;
import org.compass.core.Compass;
import org.compass.core.CompassTemplate;
import org.compass.core.config.CompassConfiguration;
import org.compass.gps.device.hibernate.CompassTransactionInterceptor;
import org.compass.gps.device.hibernate.Hibernate3GpsDevice;
import org.compass.gps.impl.SingleCompassGps;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.collection.PersistentCollection;

/**
 * @author kimchy
 */
public class CollectionNonJtaTests extends TestCase {

    private Compass compass;

    private CompassTemplate compassTemplate;

    private SingleCompassGps compassGps;

    private SessionFactory sessionFactory;

    protected void setUp() throws Exception {

        CompassConfiguration cpConf = new CompassConfiguration()
                .configure("/org/compass/gps/device/hibernate/collection/compass-nonjta.cfg.xml");
        compass = cpConf.buildCompass();
        compass.getSearchEngineIndexManager().deleteIndex();
        compass.getSearchEngineIndexManager().verifyIndex();

        compassTemplate = new CompassTemplate(compass);

        compassGps = new SingleCompassGps(compass);

        Configuration conf = new Configuration().configure("/org/compass/gps/device/hibernate/collection/hibernate-nonjta.cfg.xml")
                .setProperty(Environment.HBM2DDL_AUTO, "create");
        sessionFactory = conf.buildSessionFactory();

        CompassTransactionInterceptor.injectInterceptor(sessionFactory, new CompassTransactionInterceptor(compass));

        Hibernate3GpsDevice device = new Hibernate3GpsDevice();
        device.setSessionFactory(sessionFactory);
        device.setName("hibernateDevice");
        compassGps.addGpsDevice(device);
        compassGps.start();
    }

    public void testCollection() {
        Session s = sessionFactory.openSession();
        Transaction tx = s.beginTransaction();
        Parent parent = new Parent();
        parent.setValue("value");
        Child child1 = new Child("child1");
        parent.addChild(child1);
        Child child2 = new Child("child2");
        parent.addChild(child2);
        s.save(parent);
        tx.commit();
        s.close();

        parent = (Parent) compassTemplate.load(Parent.class, parent.getId());
        assertEquals("value", parent.getValue());

        assertEquals(1, compassTemplate.find("child1").length());

        s = sessionFactory.openSession();
        tx = s.beginTransaction();
        parent = (Parent) s.load(Parent.class, parent.getId());
        assertEquals(false, ((PersistentCollection) parent.getChilds()).wasInitialized());
        parent.setValue("newvalue");
        s.flush();
        tx.commit();
        s.close();

        s = sessionFactory.openSession();
        tx = s.beginTransaction();
        parent.setValue("newvalue1");
        s.saveOrUpdate(parent);
        tx.commit();
        s.close();
    }

    protected void tearDown() throws Exception {
        sessionFactory.close();
        compassGps.stop();
        compass.close();
    }

}
