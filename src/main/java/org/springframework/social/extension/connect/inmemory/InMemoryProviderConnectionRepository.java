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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.social.connect.ConnectionData;
import org.springframework.social.connect.ConnectionKey;
import org.springframework.social.connect.DuplicateConnectionException;


public class InMemoryProviderConnectionRepository {

	protected SortedMap<Integer, ConnectionData> connectionDataByRank = new TreeMap<Integer, ConnectionData>();

	private final String userId;
	private final String providerId;

	public InMemoryProviderConnectionRepository(String userId, String providerId) {
		this.userId = userId;
		this.providerId = providerId;
	}

	public boolean hasProviderUserId(String providerUserId) {
		return findByProviderUserId(providerUserId) != null;
	}

	public List<ConnectionData> findAllOrderByRank() {
		List<ConnectionData> connectionDatas = new ArrayList<ConnectionData>();
		connectionDatas.addAll(connectionDataByRank.values());
		return connectionDatas;
	}

	public ConnectionData findByProviderUserId(String providerUserId) {
		for (ConnectionData connectionData : connectionDataByRank.values()) {
			if (connectionData.getProviderUserId().equals(providerUserId)) {
				return connectionData;
			}
		}
		return null;
	}

	public ConnectionData findByRank(int rank) {

		return connectionDataByRank.get(rank);

	}

	public void deleteByProviderUserId(String providerUserId) {
		Integer rankOfMatchingConnectionData = null;
		for (Map.Entry<Integer, ConnectionData> connectionDataWithRank : connectionDataByRank
				.entrySet()) {
			if (connectionDataWithRank.getValue().getProviderUserId()
					.equals(providerUserId)) {
				rankOfMatchingConnectionData = connectionDataWithRank.getKey();
			}
		}
		if (rankOfMatchingConnectionData != null) {
			connectionDataByRank.remove(rankOfMatchingConnectionData);
		}

	}

	public void deleteAll() {
		connectionDataByRank = new TreeMap<Integer, ConnectionData>();
	}

	public void updateByProviderUserId(ConnectionData connection,
			String providerUserId) {
		for (Map.Entry<Integer, ConnectionData> cd : connectionDataByRank
				.entrySet()) {
			if (cd.getValue().getProviderUserId().equals(providerUserId)) {
				connectionDataByRank.put(cd.getKey(), connection);
			}
		}
	}

	

	public List<ConnectionData> findByProviderUserIdsOrderByProviderIdAndRank(
			List<String> providerUserIdsByProviderId) {
		List<ConnectionData> returnConnectionDatas = new ArrayList<ConnectionData>();
		for (ConnectionData connectionData : connectionDataByRank.values()) {
			if (providerUserIdsByProviderId.contains(connectionData
					.getProviderUserId())) {
				returnConnectionDatas.add(connectionData);
			}
		}
		return returnConnectionDatas;
	}

	public void add(ConnectionData connectionData)
			throws DuplicateConnectionException {
		for (ConnectionData cd : connectionDataByRank.values()) {
			if (cd.getProviderUserId().equals(
					connectionData.getProviderUserId())) {
				throw new DuplicateConnectionException(new ConnectionKey(
						connectionData.getProviderId(),
						connectionData.getProviderUserId()));
			}
		}
		connectionDataByRank.put(getNextRank(), connectionData);

	}
	

	public void add(ConnectionData connectionData, int rank)
			throws DuplicateConnectionException {
		for (ConnectionData cd : connectionDataByRank.values()) {
			if (cd.getProviderUserId().equals(
					connectionData.getProviderUserId())) {
				throw new DuplicateConnectionException(new ConnectionKey(
						connectionData.getProviderId(),
						connectionData.getProviderUserId()));
			}
		}
		connectionDataByRank.put(rank, connectionData);

	}

	protected int getNextRank() {
		Integer maxRank = null;
		for (Integer rank : connectionDataByRank.keySet()) {
			if (maxRank == null || rank.intValue() > maxRank.intValue()) {
				maxRank = rank.intValue();
			}
		}
		return maxRank == null ? 1 : (maxRank.intValue() + 1);
	}

}
