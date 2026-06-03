(ns katas.2-coding.core
  "Completed with Claude's help.
  https://claude.ai/share/36aa9baa-4ecf-4887-8f41-722f448a5039"
  (:require [clojure.java.io :as io]
            [clojure.test :as t]
            [katas.utils.llm-utils :refer [deftool collect-tools]]
            [katas.utils.strings :refer [<<]])
  (:import [io.github.cdimascio.dotenv Dotenv]
           [dev.langchain4j.model.ollama OllamaChatModel]
           [dev.langchain4j.service AiServices]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; load env
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def env
  (into (sorted-map)
        (for [entry (.entries (Dotenv/load))]
          [(.getKey entry) (.getValue entry)])))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; define the model
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def model
  (-> (OllamaChatModel/builder)
      (.baseUrl "http://localhost:11434")
      (.modelName "qwen2.5:7b")
      (.build)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; set up the flawed code and (correct) tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def original-file "./resources/2_coding/flawed_sort.clj")

(def original-code
  (slurp original-file))

(defn run-sort-tests []
  (let [out (new java.io.StringWriter)]
    (binding [t/*test-out* out]
      (t/run-tests 'katas.2-coding.sort-test))
    (str out)))

(defn load-and-test [code]
  (println "Testing code that is" (count code) "characters.")
  (remove-ns 'katas.2-coding.sort)
  (load-string code)
  (require 'katas.2-coding.sort-test)
  (run-sort-tests))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; create tools for the agent to manipulate and test code
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def memory
  (atom []))

(deftool test-code
  "Runs the predefined set of tests against the given Clojure code."
  [^String code]
  (load-and-test (:code code)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; coding agent
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def system-message-fn
  (reify java.util.function.Function
    (apply [_ mem-id]
      "You are a coding assistant specializing in Clojure. 
          
Your task: Fix the provided code to pass all tests.

CRITICAL: You MUST test every fix you make before considering the task complete.
1. Test the code with test-code tool
2. If tests fail, fix the code
3. Test the FIXED code with test-code tool again
4. Repeat steps 2-3 until all tests pass
5. Only return success when test-code shows all tests passing

Never assume a fix works - always verify by testing it!")))

(definterface CodingAgent
  (^String chat [^String code]))

(def agent
  (-> (AiServices/builder CodingAgent)
      (.chatModel model)
      (.tools (collect-tools test-code))
      (.systemMessageProvider system-message-fn)
      (.build)))

(println
 (.chat agent (str "Fix this code to pass all tests: " original-code)))
