spring-social-core-extension
============================

This is a custom extension module to spring-social-core.  My intention is to create pull requests for any appropriate 
additions when I get the time. 

This module currently contains:

* AbstractUsersConnectionRepositoryTest : The UsersConnectionRepository testing logic from the spring-social-core version of JdbcUsersConnectionRepositoryTest
  abstracted out into a an abstract class.  This is to enable easy testing of alternative UsersConnectionRepository implementations.

* JdbcUsersConnectionRepositoryTest  :  A subclass of AbstractUsersConnectionRepositoryTest for the JdbcUsersConnectionRepository - essentially a refactored version
  of the spring-core version which demonstrates the use of this abstract test implementation.      

* Simple map-based in-memory UsersConnectionRepository/ConnectionRepository implementations for testing/development 
  purposes, and assoicated tests.
                                                                                                                          