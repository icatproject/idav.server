Deploying the webpages
----------------------

In order to deploy these webpages containing user instructions on how to 
connect to and use the WebDAV server, simply copy the contents of the webpages
folder (and subfolders) onto a webserver.

On Glassfish, copy the files to the domain1/docroot directory.

REMINDER:

When installing the webpages describing how to connect to the webdav server, in 
order for the link to the Windows .bat file to offer the file correctly for
download, the following mime mapping needs adding to the 
domain1/config/default-web.xml file (assuming the pages are being installed on
a Glassfish server). Something similar may need doing on other webservers:

  <mime-mapping>
    <extension>bat</extension>
    <mime-type>application/x-bat</mime-type>
  </mime-mapping>

