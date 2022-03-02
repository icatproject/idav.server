Current Status
--------------
**Depecated project. No updates for several years and it is unclear that anybody is using this code.**

This is the first version of IDAV - the WebDAV interface to ICAT. It has been
produced specifically for the Artemis team within the Central Laser Facility at
STFC. As such it has a number of limitations which mean it will most likely not 
work on an existing ICAT. These differences will be addressed in future 
releases.

The main differences likely to cause problems are:

 - The hierarchy is fixed at Facility - Investigation - Dataset - Datafile
 - At the Datafile level "virtual folders" are created and used by IDAV to make
   the ICAT searches quicker. ICATs not populated via IDAV will not contain
   these folders so browsing beyond the first level of Datafile will probably
   not work.

To use IDAV on an empty ICAT, the following object need creating:

 - An Instrument
 - An InvestigationType
 - A DatasetType
 - A DatafileFormat

The names of these objects will need putting into the idav.properties file which
needs editing and putting into the Glassfish domain/config directory before the 
IDAV.war file is deployed. 
 
Some rules like the following ones will also be required for an admin user with
the ability to populate the ICAT using IDAV:

addrule idavadmins Facility CRUD
addrule idavadmins Investigation CRUD
addrule idavadmins Dataset CRUD
addrule idavadmins Datafile CRUD
addrule idavadmins Instrument R
addrule idavadmins InvestigationInstrument CRUD

addrule null DatasetType R
addrule null DatafileFormat R
addrule null InvestigationType R

Once the IDAV.war has been deployed, connect to the IDAV server following the 
instructions in the index.html page which you can find in the webpages 
directory of this distribution.

