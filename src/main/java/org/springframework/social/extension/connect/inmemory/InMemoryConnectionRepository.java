/*
 * Copyright 2011 the original author or authors.
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
package org.springframework.social.extension.connect.inmemory;

/*
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionData;
import org.springframework.social.connect.ConnectionFactory;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionKey;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.NoSuchConnectionException;
import org.springframework.social.connect.NotConnectedException;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author Michael Lavelle
 */
public class InMemoryConnectionRepository implements ConnectionRepository {

	protected final String userId;

	protected final ConnectionFactoryLocator connectionFactoryLocator;

	protected SortedMap<String, InMemoryProviderConnectionRepository> providerRepositories = new TreeMap<String, InMemoryProviderConnectionRepository>();

	public InMemoryProviderConnectionRepository getInMemoryProviderConnectionRepository(
			String providerId) {
		InMemoryProviderConnectionRepository repository = providerRepositories
				.get(providerId);
		if (repository == null) {
			repository = new InMemoryProviderConnectionRepository(userId,
					providerId);
			providerRepositories.put(providerId, repository);
		}
		return repository;
	}

	public InMemoryConnectionRepository(String userId,
			ConnectionFactoryLocator connectionFactoryLocator) {
		this.userId = userId;
		this.connectionFactoryLocator = connectionFactoryLocator;
	}
	
	

	@Override
	public MultiValueMap<String, Connection<?>> findAllConnections() {

		List<ConnectionData> connectionData = new ArrayList<ConnectionData>();

		for (Map.Entry<String, InMemoryProviderConnectionRepository> providerConnectionRepository : providerRepositories
				.entrySet()) {
			connectionData.addAll(providerConnectionRepository.getValue()
					.findAllOrderByRank());
		}
		List<Connection<?>> resultList = createConnections(connectionData);

		MultiValueMap<String, Connection<?>> connections = new LinkedMultiValueMap<String, Connection<?>>();
		Set<String> registeredProviderIds = connectionFactoryLocator
				.registeredProviderIds();
		for (String registeredProviderId : registeredProviderIds) {
			connections.put(registeredProviderId,
					Collections.<Connection<?>> emptyList());
		}
		for (Connection<?> connection : resultList) {
			String providerId = connection.getKey().getProviderId();
			if (connections.get(providerId).size() == 0) {
				connections.put(providerId, new LinkedList<Connection<?>>());
			}
			connections.add(providerId, connection);
		}

		return connections;
	}

	@Override
	public List<Connection<?>> findConnections(String providerId) {
		return createConnections(getInMemoryProviderConnectionRepository(providerId)
				.findAllOrderByRank());

	}

	@Override
	@SuppressWarnings("unchecked")
	public <A> List<Connection<A>> findConnections(Class<A> apiType) {
		List<?> connections = findConnections(getProviderId(apiType));
		return (List<Connection<A>>) connections;
	}

	@Override
	public MultiValueMap<String, Connection<?>> findConnectionsToUsers(
			MultiValueMap<String, String> providerUsers) {
		if (providerUsers == null || providerUsers.isEmpty()) {
			throw new IllegalArgumentException(
					"Unable to execute find: no providerUsers provided");
		}

		Map<String, List<String>> providerUserIdsByProviderId = new HashMap<String, List<String>>();
		for (Iterator<Entry<String, List<String>>> it = providerUsers
				.entrySet().iterator(); it.hasNext();) {
			Entry<String, List<String>> entry = it.next();
			String providerId = entry.getKey();
			providerUserIdsByProviderId.put(providerId, entry.getValue());
		}

		List<ConnectionData> connectionDatas = new ArrayList<ConnectionData>();
		for (Map.Entry<String, List<String>> entry : providerUserIdsByProviderId
				.entrySet()) {
			connectionDatas.addAll(getInMemoryProviderConnectionRepository(
					entry.getKey())
					.findByProviderUserIdsOrderByProviderIdAndRank(
							entry.getValue()));
		}

		List<Connection<?>> resultList = createConnections(connectionDatas);
		MultiValueMap<String, Connection<?>> connectionsForUsers = new LinkedMultiValueMap<String, Connection<?>>();
		for (Connection<?> connection : resultList) {
			String providerId = connection.getKey().getProviderId();
			List<String> userIds = providerUsers.get(providerId);
			List<Connection<?>> connections = connectionsForUsers
					.get(providerId);
			if (connections == null) {
				connections = new ArrayList<Connection<?>>(userIds.size());
				for (int i = 0; i < userIds.size(); i++) {
					connections.add(null);
				}
				connectionsForUsers.put(providerId, connections);
			}
			String providerUserId = connection.getKey().getProviderUserId();
			int connectionIndex = userIds.indexOf(providerUserId);
			connections.set(connectionIndex, connection);
		}
		return connectionsForUsers;
	}

	@Override
	public Connection<?> getConnection(ConnectionKey connectionKey) {

		ConnectionData connectionData = getInMemoryProviderConnectionRepository(
				connectionKey.getProviderId()).findByProviderUserId(
				connectionKey.getProviderUserId());

		if (connectionData == null) {
			throw new NoSuchConnectionException(connectionKey);
		} else {
			return createConnection(connectionData);
		}

	}

	@Override
	@SuppressWarnings("unchecked")
	public <A> Connection<A> getConnection(Class<A> apiType,
			String providerUserId) {
		String providerId = getProviderId(apiType);
		return (Connection<A>) getConnection(new ConnectionKey(providerId,
				providerUserId));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <A> Connection<A> getPrimaryConnection(Class<A> apiType) {
		String providerId = getProviderId(apiType);
		Connection<A> connection = (Connection<A>) findPrimaryConnection(providerId);
		if (connection == null) {
			throw new NotConnectedException(providerId);
		}
		return connection;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <A> Connection<A> findPrimaryConnection(Class<A> apiType) {
		String providerId = getProviderId(apiType);
		return (Connection<A>) findPrimaryConnection(providerId);
	}

	@Override
	public void removeConnections(String providerId) {
		getInMemoryProviderConnectionRepository(providerId).deleteAll();
	}

	@Override
	public void removeConnection(ConnectionKey connectionKey) {
		getInMemoryProviderConnectionRepository(connectionKey.getProviderId())
				.deleteByProviderUserId(connectionKey.getProviderUserId());
	}

	@Override
	public void addConnection(Connection<?> connection) {
		ConnectionData connectionData = connection.createData();
		getInMemoryProviderConnectionRepository(connectionData.getProviderId())
				.add(connectionData);
	}

	@Override
	public void updateConnection(Connection<?> connection) {
		ConnectionData data = connection.createData();
		getInMemoryProviderConnectionRepository(data.getProviderId())
				.updateByProviderUserId(data, data.getProviderUserId());
	}

	private Connection<?> findPrimaryConnection(String providerId) {

		ConnectionData connectionData = getInMemoryProviderConnectionRepository(
				providerId).findByRank(1);
		if (connectionData != null) {
			return createConnection(connectionData);
		} else {
			return null;
		}
	}

	protected Connection<?> createConnection(ConnectionData connectionData) {
		ConnectionFactory<?> connectionFactory = connectionFactoryLocator
				.getConnectionFactory(connectionData.getProviderId());
		return connectionFactory.createConnection(connectionData);
	}

	protected List<Connection<?>> createConnections(
			List<ConnectionData> connectionDataList) {
		List<Connection<?>> connections = new ArrayList<Connection<?>>();
		for (ConnectionData connectionData : connectionDataList) {
			connections.add(createConnection(connectionData));
		}
		return connections;
	}

	private <A> String getProviderId(Class<A> apiType) {
		return connectionFactoryLocator.getConnectionFactory(apiType)
				.getProviderId();
	}


	public void addConnectionData(ConnectionData connectionData, int rank) {
		getInMemoryProviderConnectionRepository(connectionData.getProviderId())
				.add(connectionData, rank);
	}

	protected boolean hasConnection(ConnectionKey key) {
		InMemoryProviderConnectionRepository providerConnectionRepository = getInMemoryProviderConnectionRepository(key
				.getProviderId());
		return providerConnectionRepository.hasProviderUserId(key
				.getProviderUserId());
	}

}