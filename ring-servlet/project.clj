(defproject mourjo/ring-servlet "1.0.0-1.8.1"
  :description "Ring servlet utilities."
  :url "https://github.com/mourjo/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [mourjo/ring-core "1.0.0-1.8.1"]]
  :aliases {"test-all" ["with-profile" "default:+1.8:+1.9:+1.10" "test"]}
  :profiles
  {:provided {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]]}
   :dev  {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]]}
   :1.8  {:dependencies [[org.clojure/clojure "1.8.0"]]}
   :1.9  {:dependencies [[org.clojure/clojure "1.9.0"]]}
   :1.10 {:dependencies [[org.clojure/clojure "1.10.1"]]}})
