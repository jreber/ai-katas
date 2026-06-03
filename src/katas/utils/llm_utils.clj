(ns katas.utils.llm-utils
  (:require [katas.utils.strings :refer [<<]])
  (:import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
           dev.langchain4j.data.document.parser.TextDocumentParser
           dev.langchain4j.data.document.splitter.DocumentByLineSplitter
           dev.langchain4j.model.chat.request.DefaultChatRequestParameters
           [dev.langchain4j.data.message SystemMessage UserMessage]))


(defn configs
  ([]
   (configs "./resources/env.env"))
  ([env-file]
   (->> env-file
        slurp
        clojure.string/split-lines
        (remove (comp clojure.string/blank? clojure.string/trim))
        (map #(clojure.string/split % #"="))
        (into (sorted-map)))))

(defn spy [x]
  (println "~~~~~~~~~~ SPYING ~~~~~~~~~~\n\n" x "\n\n")
  x)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; syntactic sugar around tooling
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro deftool
  "Define a Langchain4j tool function.
  
  Arguments:
    name - the name for the tool function
    description - a string description of the tool (used by the LLM)
    args - argument vector (each arg should be typed with ^String)
    & body - function body, should return a String
  
  Example:
    (deftool brainstorming-tool
      \"Brainstorming function to generate ideas for a problem.\"
      [^String role ^String problem]
      (str \"Brainstorming as \" role \" for problem: \" problem))
  
  Returns a vector of [ToolSpecification TaskExecutor] ready for use with collect-tools."
  [name description args & body]
  (let [impl-fn-name (symbol (str name "-impl"))
        arg-count (count args)
        arg-names (mapv (fn [arg] (if (symbol? arg) arg (symbol (str arg)))) args)]
    (require 'cheshire.core)
    `(do
       ;; Define the implementation function
       (defn ~impl-fn-name ~args ~@body)
       
       ;; Create a ToolSpecification and ToolExecutor
       (def ~name
         (let [tool-spec# (-> (dev.langchain4j.agent.tool.ToolSpecification/builder)
                              (.name ~(str name))
                              (.description ~description)
                              (.build))
               tool-executor# (reify dev.langchain4j.service.tool.ToolExecutor
                                (execute [this# tool-input# tool-memory-id#]
                                  (str
                                   (~impl-fn-name
                                    (cheshire.core/parse-string (.arguments tool-input#) true)))))]
           [tool-spec# tool-executor#]))
       
       ;; Return the tool vector
       ~name)))

(defn collect-tools
  "Collect tool vectors into a Java HashMap for use with AiServices/tools(Map)."
  [& tools]
  (let [map (java.util.HashMap.)]
    (doseq [[spec executor] tools]
      (.put map spec executor))
    map))
