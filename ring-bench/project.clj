(defproject mourjo/ring-bench "1.0.0-1.8.1"
  :description "Ring core libraries."
  :url "https://github.com/mourjo/ring"
  :scm {:dir ".."}
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [jmh-clojure "0.2.1"]
                 [mourjo/ring-jetty-adapter "1.0.0-1.8.1"]
                 [mourjo/ring-servlet "1.0.0-1.8.1"]]
  :jvm-opts {}
  :main ring.bench.servlet)
