-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.expressionLanguage-4.0
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=io.openliberty.el.internal.cdi.jakarta, \
 io.openliberty.jakarta.expressionLanguage.4.0; location:="dev/api/spec/,lib/"; mavenCoordinates="org.apache.tomcat:tomcat-el-api:10.0.14"
kind=ga
edition=core
WLP-Activation-Type: parallel
