(defproject mourjo/ring-jetty-adapter "1.0.0-1.8.1"
  :description "Ring Jetty adapter."
  :url "https://github.com/ring-clojure/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [mourjo/ring-core "1.0.0-1.8.1"]
                 [mourjo/ring-servlet "1.0.0-1.8.1"]
                 [org.eclipse.jetty/jetty-server "9.4.28.v20200408"]]
  :aliases {"test-all" ["with-profile" "default:+1.8:+1.9:+1.10" "test"]}
  :profiles
  {:dev  {:dependencies [[clj-http "3.10.0"]]
          :jvm-opts ["-Dorg.eclipse.jetty.server.HttpChannelState.DEFAULT_TIMEOUT=500"]}
   :1.8  {:dependencies [[org.clojure/clojure "1.8.0"]]}
   :1.9  {:dependencies [[org.clojure/clojure "1.9.0"]]}
   :1.10 {:dependencies [[org.clojure/clojure "1.10.1"]]}})
