/**
 * Licensed to Apereo under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership. Apereo
 * licenses this file to you under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the License at the
 * following location:
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apereo.portal.io.xml.subscribedfragment;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apereo.portal.IUserIdentityStore;
import org.apereo.portal.fragment.subscribe.IUserFragmentSubscription;
import org.apereo.portal.fragment.subscribe.dao.IUserFragmentSubscriptionDao;
import org.apereo.portal.io.xml.AbstractJaxbDataHandler;
import org.apereo.portal.io.xml.IPortalData;
import org.apereo.portal.io.xml.IPortalDataType;
import org.apereo.portal.io.xml.PortalDataKey;
import org.apereo.portal.io.xml.SimpleStringPortalData;
import org.apereo.portal.security.IPerson;
import org.apereo.portal.utils.SafeFilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

public class SubscribedFragmentImporterExporter
        extends AbstractJaxbDataHandler<ExternalSubscribedFragments> {

    private SubscribedFragmentPortalDataType subscribedFragmentPortalDataType;
    private IUserFragmentSubscriptionDao userFragmentSubscriptionDao;
    private IUserIdentityStore userIdentityStore;
    private boolean errorOnMissingUser = true;

    @Autowired
    public void setSubscribedFragmentPortalDataType(
            SubscribedFragmentPortalDataType subscribedFragmentPortalDataType) {
        this.subscribedFragmentPortalDataType = subscribedFragmentPortalDataType;
    }

    @Autowired
    public void setUserFragmentSubscriptionDao(
            IUserFragmentSubscriptionDao userFragmentSubscriptionDao) {
        this.userFragmentSubscriptionDao = userFragmentSubscriptionDao;
    }

    @Autowired
    public void setUserIdentityStore(IUserIdentityStore userIdentityStore) {
        this.userIdentityStore = userIdentityStore;
    }

    @Value("${org.apereo.portal.io.layout.errorOnMissingUser:true}")
    public void setErrorOnMissingUser(boolean errorOnMissingUser) {
        this.errorOnMissingUser = errorOnMissingUser;
    }

    @Override
    public Set<PortalDataKey> getImportDataKeys() {
        return Collections.singleton(SubscribedFragmentPortalDataType.IMPORT_40_DATA_KEY);
    }

    @Override
    public IPortalDataType getPortalDataType() {
        return this.subscribedFragmentPortalDataType;
    }

    @Override
    public Iterable<? extends IPortalData> getPortalData() {
        final List<String> allUsersWithActiveSubscriptions =
                this.userFragmentSubscriptionDao.getAllUsersWithActiveSubscriptions();

        return Collections2.transform(
                allUsersWithActiveSubscriptions,
                new Function<String, IPortalData>() {
                    @Override
                    public IPortalData apply(String input) {
                        return new SimpleStringPortalData(input, null, null);
                    }
                });
    }

    @Transactional
    @Override
    public void importData(ExternalSubscribedFragments data) {
        final String username = data.getUsername();
        final IPerson person = getPerson(username, true);

        for (final SubscribedFragmentType subscribedFragmentType : data.getSubscribedFragments()) {
            final String fragmentOwner = subscribedFragmentType.getFragmentOwner();
            final IPerson fragmentPerson = getPerson(fragmentOwner, false);
            if (fragmentPerson == null) {
                throw new IllegalArgumentException(
                        "No fragmentOwner "
                                + fragmentOwner
                                + " exists to subscribe to, be sure to import all fragment owners first");
            }

            final IUserFragmentSubscription userFragmentSubscription =
                    this.userFragmentSubscriptionDao.getUserFragmentInfo(person, fragmentPerson);
            if (userFragmentSubscription == null) {
                this.userFragmentSubscriptionDao.createUserFragmentInfo(person, fragmentPerson);
            }
        }
    }

    private IPerson getPerson(final String username, boolean create) {
        IPerson rslt;
        try {
            // Try once w/ false, even if create=true...
            rslt = userIdentityStore.getPerson(username, false);
        } catch (final Exception e) {
            if (!create || this.errorOnMissingUser) {
                throw new RuntimeException(
                        "Unrecognized user '"
                                + username
                                + "'; you must import users before their layouts.",
                        e);
            }
            rslt = userIdentityStore.getPerson(username, true);
        }
        return rslt;
    }

    /*
     * (non-Javadoc)
     * @see org.apereo.portal.io.xml.IDataImporterExporter#exportData(java.lang.String)
     */
    @Override
    public ExternalSubscribedFragments exportData(String id) {
        final IPerson person = this.getPerson(id, false);
        if (person == null) {
            //No user to export for
            return null;
        }

        return exportInternal(person);
    }

    private ExternalSubscribedFragments exportInternal(final IPerson person) {
        final ExternalSubscribedFragments data = new ExternalSubscribedFragments();
        data.setUsername(person.getUserName());

        final List<SubscribedFragmentType> subscribedFragments = data.getSubscribedFragments();

        for (final IUserFragmentSubscription userFragmentSubscription :
                this.userFragmentSubscriptionDao.getUserFragmentInfo(person)) {
            if (userFragmentSubscription.isActive()) {
                final SubscribedFragmentType subscribedFragmentType = new SubscribedFragmentType();
                subscribedFragmentType.setFragmentOwner(
                        userFragmentSubscription.getFragmentOwner());
                subscribedFragments.add(subscribedFragmentType);
            }
        }

        if (subscribedFragments.isEmpty()) {
            return null;
        }

        Collections.sort(subscribedFragments, SubscribedFragmentTypeComparator.INSTANCE);

        return data;
    }

    @Override
    public String getFileName(ExternalSubscribedFragments data) {
        return SafeFilenameUtils.makeSafeFilename(data.getUsername());
    }

    /*
     * (non-Javadoc)
     * @see org.apereo.portal.io.xml.IDataImporterExporter#deleteData(java.lang.String)
     */
    @Transactional
    @Override
    public ExternalSubscribedFragments deleteData(String id) {
        final IPerson person = this.getPerson(id, false);
        if (person == null) {
            //Nothing to delete
            return null;
        }

        final ExternalSubscribedFragments data = exportInternal(person);

        for (final IUserFragmentSubscription userFragmentSubscription :
                this.userFragmentSubscriptionDao.getUserFragmentInfo(person)) {
            this.userFragmentSubscriptionDao.deleteUserFragmentInfo(userFragmentSubscription);
        }

        return data;
    }
}
