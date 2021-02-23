(ns {{namespace}}.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [matcho.core :refer [match]]
            [{{namespace}}.core :as sut]))


(deftest ^:unit a-test
  (testing "simple test."
    (is (= 1 1))
    (match {:a 1} {:a int?})))
