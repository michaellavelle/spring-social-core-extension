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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionData;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionKey;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.ConnectionSignUp;
import org.springframework.social.connect.UsersConnectionRepository;

/**
 * {@link UsersConnectionRepository} that stores Connection data in a simple
 * in-memory map structure
 * 
 * @author Michael Lavelle
 */
public class InMemoryUsersConnectionRepository implements
		UsersConnectionRepository {

	protected final ConnectionFactoryLocator connectionFactoryLocator;

	private ConnectionSignUp connectionSignUp;

	protected SortedMap<String, InMemoryConnectionRepository> connectionRepositoriesByUserId;

	public void addConnectionData(String userId, ConnectionData connectionData,
			int rank) {
		createInMemoryConnectionRepository(userId).addConnectionData(
				connectionData, rank);
	}

	public InMemoryUsersConnectionRepository(
			ConnectionFactoryLocator connectionFactoryLocator) {
		this.connectionFactoryLocator = connectionFactoryLocator;
		this.connectionRepositoriesByUserId = new TreeMap<String, InMemoryConnectionRepository>();
	}

	/**
	 * The command to execute to create a new local user profile in the event no
	 * user id could be mapped to a connection. Allows for implicitly creating a
	 * user profile from connection data during a provider sign-in attempt.
	 * Defaults to null, indicating explicit sign-up will be required to
	 * complete the provider sign-in attempt.
	 * 
	 * @see #findUserIdsWithConnection(Connection)
	 */
	public void setConnectionSignUp(ConnectionSignUp connectionSignUp) {
		this.connectionSignUp = connectionSignUp;
	}

	public List<String> findUserIdsWithConnection(Connection<?> connection) {
		ConnectionKey key = connection.getKey();
		List<String> localUserIds = new ArrayList<String>();
		for (Map.Entry<String, InMemoryConnectionRepository> entry : connectionRepositoriesByUserId
				.entrySet()) {
			if (entry.getValue().hasConnection(key)) {
				localUserIds.add(entry.getKey());
			}
		}
		if (localUserIds.size() == 0 && connectionSignUp != null) {
			String newUserId = connectionSignUp.execute(connection);
			if (newUserId != null) {
				createConnectionRepository(newUserId).addConnection(connection);
				return Arrays.asList(newUserId);
			}
		}
		return localUserIds;
	}

	public Set<String> findUserIdsConnectedTo(String providerId,
			Set<String> providerUserIds) {
		final Set<String> localUserIds = new HashSet<String>();
		for (Map.Entry<String, InMemoryConnectionRepository> entry : connectionRepositoriesByUserId
				.entrySet()) {
			for (String providerUserId : providerUserIds) {
				if (entry.getValue().hasConnection(
						new ConnectionKey(providerId, providerUserId))) {
					localUserIds.add(entry.getKey());
				}
			}
		}
		return localUserIds;
	}

	public ConnectionRepository createConnectionRepository(String userId) {
		if (userId == null) {
			throw new IllegalArgumentException("userId cannot be null");
		}
		return createInMemoryConnectionRepository(userId);
	}

	protected InMemoryConnectionRepository createInMemoryConnectionRepository(
			String userId) {
		InMemoryConnectionRepository connectionRepository = connectionRepositoriesByUserId
				.get(userId);
		if (connectionRepository == null) {
			connectionRepository = new InMemoryConnectionRepository(userId,
					connectionFactoryLocator);
			connectionRepositoriesByUserId.put(userId, connectionRepository);
		}

		return connectionRepository;
	}

}