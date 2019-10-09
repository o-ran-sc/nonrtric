======================
Introduction
======================
You have generated an MD-SAL module using the Brocade Archetype. 

* You should be able to successfully run 'mvn clean install' on this project.
* This will produce a .zip file under the karaf.extension directory which you can deploy using
Brocade's extension deployment mechanism.

======================
Next Steps:
======================
* run a 'mvn clean install' if you haven't already. This will generate some code from the yang models.
* Modify the model yang file under the model project.
* Follow the comments in the generated provider class to wire your new provider into the generated 
code.
* Modify the generated provider model to respond to and handle the yang model. Depending on what
you added to your model you may need to inherit additional interfaces or make other changes to
the provider model.

======================
Generated Bundles:
======================
* model
    - Provides the yang model for your application. This is your primary northbound interface.
* provider
    - Provides a template implementation for a provider to respond to your yang model.
* features
    - Defines a karaf feature. If you add dependencies on third-party bundles then you will need to
      modify the features.xml to list out the dependencies.
* karaf.extension
    - Bundles all of the jars and third party dependencies (minus ODL dependencies) into a single
      .zip file with the necessary configuration files to work correctly with the Brocade extension
      mechanism.
      
