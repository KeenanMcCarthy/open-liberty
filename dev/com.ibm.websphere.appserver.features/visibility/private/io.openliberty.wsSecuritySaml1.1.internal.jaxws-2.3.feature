-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.wsSecuritySaml1.1.internal.jaxws-2.3
singleton=true
visibility = private
-features=com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0", \
  com.ibm.websphere.appserver.jaxws-2.3, \
  io.openliberty.samlWeb2.0.internal.opensaml-3.4
kind=noship
edition=full
