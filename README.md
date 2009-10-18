
DeepaMehta 2 Poemspace Exporter
===============================

This web application demonstrates how to export content from a DeepaMehta 2 installation
in order to import it to DeepaMehta 3.

*Note*  
This is not a universal DeepaMehta 2 exporter.
DeepaMehta 2 Poemspace Exporter is tailored for a specific custom data model.
Without modification this will not run with your DeepaMehta 2 installation.

However, if you plan to transfer DeepaMehta 2 content to DeepaMehta 3
this application might serve you as a good starting point.


Requirements
------------

* A DeepaMehta 2 installation  
  <http://www.deepamehta.de>

* Tomcat (or another Servlet engine)  
  <http://tomcat.apache.org/>

* Ant
  <http://ant.apache.org/>


Installation
------------

1.  Clone the Git repository to your computer
        git clone git://github.com/jri/dm2-poemspace-exporter.git
    A directory `dm2-poemspace-exporter` will be created. Go there:
        cd dm2-poemspace-exporter

2.  Configure two servlet context parameters in file `web.xml` (found at top-level):  
    `home` - the home directory of your DeepaMehta installation, e.g. `/usr/local/deepamehta2`.  
    `service` - the DeepaMehta instance you want to export, e.g. `default`.

3.  Build with ant
        ant
    The file `dist/poemspace-exporter.war` will be created.

4.  Move `poemspace-exporter.war` to your Tomcat `webapps` directory, e.g.:
        mv dist/poemspace-exporter.war /var/lib/tomcat/webapps


Usage
-----

1.  Start the export process by pointing your webbrowser to:
        http://localhost:8080/poemspace-exporter/export
    (replace `localhost:8080` with your Tomcat host and port.)

    No further interaction is required. The export process might take a while
    (that's why the session timeout is increased to 5 hours in `web.xml`).
    When its done, the webbrowser shows a page with the exported data in JSON format.

2.  In order to see possible special characters properly on the page,
    you must set the browser display to UTF-8 encoding.
    
3.  Save the page to a text file.

4.  You can import that file to DeepaMehta 3 by means of the "DM3 Import" plugin.
    <http://github.com/jri/dm3-import>


------------
Jörg Richter  
Oct 18, 2009
