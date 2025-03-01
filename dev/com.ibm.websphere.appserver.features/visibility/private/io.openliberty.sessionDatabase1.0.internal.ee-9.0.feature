-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.sessionDatabase1.0.internal.ee-9.0
singleton=true
visibility = private

-features=\
  io.openliberty.servlet.api-5.0; apiJar=false; ibm.tolerates:="6.0"

-bundles= com.ibm.ws.session.jakarta, \
  		  com.ibm.ws.session.db.jakarta, \
  		  com.ibm.ws.session.store.jakarta

kind=ga
edition=core

