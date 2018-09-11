(defproject brosenan/rabbit-drivers "0.2.1-SNAPSHOT"
  :description "InjectTheDriver driver implementations for RabbitMQ"
  :url "https://github.com/brosenan/rabbit-drivers"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [brosenan/injectthedriver "0.0.5"]
                 [com.novemberain/langohr "5.0.0"]
                 [brosenan/lambdakube "0.8.1"]]
  :profiles {:dev {:dependencies [[midje "1.9.2"]]
                   :plugins [[lein-midje "3.2.1"]]}}
  :aot :all
  :deploy-repositories [["releases" :clojars]])
