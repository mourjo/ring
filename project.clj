(defproject mourjo/ring "1.0.0-1.8.1"
  :description "A Clojure web applications library."
  :url "https://github.com/mourjo/ring"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[mourjo/ring-core "1.0.0-1.8.1"]
                 [mourjo/ring-devel "1.0.0-1.8.1"]
                 [mourjo/ring-jetty-adapter "1.0.0-1.8.1"]
                 [mourjo/ring-servlet "1.0.0-1.8.1"]]
  :plugins [[lein-sub "0.3.0"]
            [lein-codox "0.10.3"]]
  :sub ["ring-core"
        "ring-devel"
        "ring-jetty-adapter"
        "ring-servlet"]
  :codox {:output-path "codox"
          :source-uri "http://github.com/mourjo/ring/blob/{version}/{filepath}#L{line}"
          :source-paths ["ring-core/src"
                         "ring-devel/src"
                         "ring-jetty-adapter/src"
                         "ring-servlet/src"]}
  :aliases {"test"     ["sub" "test"]
            "test-all" ["sub" "test-all"]
            "bench"    ["sub" "-s" "ring-bench" "run"]})
