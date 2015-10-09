How to run the tests
--------------------

The JUnit tests are not unit tests but functional tests and can currently only
be run from a Windows PC.

To run the tests, a network drive needs mapping to the IDAV server to be tested
using an account that has CRUD permissions on Facility, Investigation, Dataset
and Datafile.

The drive letter of the mapped network drive for IDAV needs changing in the 
test.properties file along with any of the other test parameters that might 
need customising.
