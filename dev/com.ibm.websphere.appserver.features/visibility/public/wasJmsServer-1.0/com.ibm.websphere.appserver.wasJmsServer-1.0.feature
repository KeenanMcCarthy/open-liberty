-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.wasJmsServer-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-API-Package: com.ibm.websphere.messaging.mbean; type="ibm-api"
IBM-ShortName: wasJmsServer-1.0
Subsystem-Name: Message Server 1.0
-features=\
  com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0, 8.0", \
  com.ibm.websphere.appserver.appLifecycle-1.0, \
  com.ibm.websphere.appserver.channelfw-1.0, \
  com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:="1.2"
-bundles=com.ibm.ws.messaging.comms.server, \
 com.ibm.ws.messaging.msgstore, \
 com.ibm.ws.messaging.common, \
 com.ibm.ws.messaging.utils, \
 com.ibm.ws.messaging.security.common, \
 com.ibm.ws.messaging.runtime, \
 com.ibm.ws.messaging.comms.client, \
 com.ibm.websphere.security
-jars=com.ibm.websphere.appserver.api.messaging; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.messaging_1.0-javadoc.zip
kind=ga
edition=base
