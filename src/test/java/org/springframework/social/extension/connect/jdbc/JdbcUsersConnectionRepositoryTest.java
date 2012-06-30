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
package org.springframework.social.extension.connect.jdbc;

import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.h2.Driver;
import org.junit.After;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.ConnectionProperties;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseConfigurer;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.social.connect.ConnectionSignUp;
import org.springframework.social.connect.jdbc.JdbcUsersConnectionRepository;

/**
 * @author Michael Lavelle
 */

public class JdbcUsersConnectionRepositoryTest extends
		AbstractUsersConnectionRepositoryTest<JdbcUsersConnectionRepository> {

	private JdbcTemplate dataAccessor;
	private EmbeddedDatabase database;

	private boolean testMySqlCompatiblity;

	protected String getSchemaSql() {
		return "JdbcUsersConnectionRepository.sql";
	}

	@Override
	protected JdbcUsersConnectionRepository createUsersConnectionRepository() {
		EmbeddedDatabaseFactory factory = new EmbeddedDatabaseFactory();
		if (testMySqlCompatiblity) {
			factory.setDatabaseConfigurer(new MySqlCompatibleH2DatabaseConfigurer());
		} else {
			factory.setDatabaseType(EmbeddedDatabaseType.H2);
		}
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.addScript(new ClassPathResource(getSchemaSql(), getClass()));
		factory.setDatabasePopulator(populator);
		database = factory.getDatabase();
		dataAccessor = new JdbcTemplate(database);
		JdbcUsersConnectionRepository usersConnectionRepository = new JdbcUsersConnectionRepository(
				database, connectionFactoryRegistry, Encryptors.noOpText());
		if (!getTablePrefix().equals("")) {
			usersConnectionRepository.setTablePrefix(getTablePrefix());
		}
		return usersConnectionRepository;
	}

	@Override
	protected void setConnectionSignUpOnUsersConnectionRepository(
			JdbcUsersConnectionRepository usersConnectionRepository,
			ConnectionSignUp connectionSignUp) {
		usersConnectionRepository.setConnectionSignUp(connectionSignUp);
	}

	@After
	public void tearDown() {
		if (database != null) {
			database.shutdown();
		}
	}

	@Override
	protected void insertConnection(String userId, String providerId,
			String providerUserId, int rank, String displayName,
			String profileUrl, String imageUrl, String accessToken,
			String secret, String refreshToken, Long expireTime) {

		dataAccessor
				.update("insert into "
						+ getTablePrefix()
						+ "UserConnection (userId, providerId, providerUserId, rank, displayName, profileUrl, imageUrl, accessToken, secret, refreshToken, expireTime) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
						userId, providerId, providerUserId, rank, displayName,
						profileUrl, imageUrl, accessToken, secret, null, null);

	}

	private static class MySqlCompatibleH2DatabaseConfigurer implements
			EmbeddedDatabaseConfigurer {
		public void shutdown(DataSource dataSource, String databaseName) {
			try {
				java.sql.Connection connection = dataSource.getConnection();
				Statement stmt = connection.createStatement();
				stmt.execute("SHUTDOWN");
			} catch (SQLException ex) {
			}
		}

		public void configureConnectionProperties(
				ConnectionProperties properties, String databaseName) {
			properties.setDriverClass(Driver.class);
			properties.setUrl(String
					.format("jdbc:h2:mem:%s;MODE=MYSQL;DB_CLOSE_DELAY=-1",
							databaseName));
			properties.setUsername("sa");
			properties.setPassword("");
		}
	}

	@Override
	protected Boolean checkIfProviderConnectionsExist(String providerId) {
		return dataAccessor.queryForObject("select exists (select 1 from "
				+ getTablePrefix() + "UserConnection where providerId = '"
				+ providerId + "')", Boolean.class);
	}

}
