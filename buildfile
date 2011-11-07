require 'buildr/scala'

# Generated by Buildr 1.4.4, change to your liking
# Version number for this release
VERSION_NUMBER = "0.0.1"
# Group identifier for your projects
GROUP = "frogfish-scala"
COPYRIGHT = ""

# Specify Maven 2.0 remote repositories here, like this:
#repositories.remote << "http://www.ibiblio.org/maven2/"
repositories.remote << "http://repo1.maven.org/maven2"
repositories.remote << "http://no-commons-logging.zapto.org/mvn2"
repositories.remote << "http://repository.jboss.org/maven2"

module Frogfish
  COMPILE_LIBRARIES = []
  # LOGGING
  COMPILE_LIBRARIES << 'commons-logging:commons-logging:jar:99.0-does-not-exist'
  COMPILE_LIBRARIES << 'org.slf4j:jcl-over-slf4j:jar:1.6.4'
  COMPILE_LIBRARIES << 'org.slf4j:slf4j-api:jar:1.6.4'
  COMPILE_LIBRARIES << 'ch.qos.logback:logback-core:jar:1.0.0'
  COMPILE_LIBRARIES << 'ch.qos.logback:logback-classic:jar:1.0.0'

  # ESPER
  COMPILE_LIBRARIES << 'com.espertech:esper:jar:4.4.0'

  # AMQ
  COMPILE_LIBRARIES << 'org.apache.activemq:activemq-core:jar:5.5.1'

  # JSON
  COMPILE_LIBRARIES << 'org.codehaus.jackson:jackson-core-asl:jar:1.9.2'
  COMPILE_LIBRARIES << 'org.codehaus.jackson:jackson-mapper-asl:jar:1.9.2'

  # CORE & COMMON
  COMPILE_LIBRARIES << 'javax.jms:jms:jar:1.1'
  COMPILE_LIBRARIES << 'commons-lang:commons-lang:jar:2.6'
  COMPILE_LIBRARIES << '../FrogfishUtil/dist/lib/FrogfishUtil-0.0.3-1.jar'

  RUN_LIBRARIES = COMPILE_LIBRARIES.dup

  HOSTNAME=`hostname`.chomp
  DATE=Time.now.strftime("%Y%m%d%H%M%S")
  JMX_PORT = 4679
  HEAP_MIN="512m"
  HEAP_MAX="512m"
  GC_LOG="/Users/tonio/Desktop/frogfish-scala.gc.#{DATE}.log"

  JAVA_ARGS = []
  JAVA_ARGS << "-server"
  JAVA_ARGS << "-Dcom.sun.management.jmxremote"
  JAVA_ARGS << "-Dcom.sun.management.jmxremote.port=#{JMX_PORT}"
  JAVA_ARGS << "-Dcom.sun.management.jmxremote.ssl=false"
  JAVA_ARGS << "-Dcom.sun.management.jmxremote.authenticate=false"
  JAVA_ARGS << "-Djava.rmi.server.hostname=#{HOSTNAME}"
  JAVA_ARGS << "-XX:NewRatio=2"
  JAVA_ARGS << "-Xms#{HEAP_MIN}"
  JAVA_ARGS << "-Xmx#{HEAP_MAX}"
  JAVA_ARGS << "-Xloggc:#{GC_LOG}"
  JAVA_ARGS << "-XX:+PrintGCDetails"
end

desc "The Frogfish-scala project"
define "frogfish-scala" do
  project.version = VERSION_NUMBER
  project.group = GROUP
  manifest["Implementation-Vendor"] = COPYRIGHT
  Frogfish::COMPILE_LIBRARIES.each { |x| compile.with transitive(x) }
  Frogfish::RUN_LIBRARIES.each { |x| run.with transitive(x) }

  run.using :main => "com.antonioalegria.frogfish.Frogfish", :java_args => Frogfish::JAVA_ARGS

  package :jar
end
