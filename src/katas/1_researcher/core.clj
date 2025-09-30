(ns katas.1-researcher.core
  "Completed with Claude's help.
  https://claude.ai/share/36aa9baa-4ecf-4887-8f41-722f448a5039"
  (:import [io.github.cdimascio.dotenv Dotenv]
           [dev.langchain4j.model.ollama OllamaChatModel]
           [dev.langchain4j.service AiServices]
           [dev.langchain4j.mcp.client DefaultMcpClient$Builder]
           [dev.langchain4j.mcp.client.transport.stdio StdioMcpTransport$Builder]
           [dev.langchain4j.mcp McpToolProvider]))


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

#_(.generate model "Say hello in one sentence.")


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; define research tools
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def brave-transport
  (-> (StdioMcpTransport$Builder.)  ;; Use inner Builder class
      (.command (java.util.List/of "npx" "-y" "@modelcontextprotocol/server-brave-search"))
      (.environment {"BRAVE_API_KEY" (env "BRAVE_API_KEY")})
      (.logEvents true)
      (.build)))

(def brave-mcp-client
  (-> (DefaultMcpClient$Builder.)
      (.key "brave-search")
      (.transport brave-transport)
      (.build)))

;; Tool Provider
(def brave-tool-provider
  (-> (McpToolProvider/builder)
      (.mcpClients (into-array [brave-mcp-client]))
      (.build)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; define the agent
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def system-message-fn
  (reify java.util.function.Function
    (apply [_ mem-id]
      "You are a research assistant. 
       
       IMPORTANT: Search at least 3 different queries before synthesizing.
       
       Format your final answer as:
       ## Summary
       [Your synthesis here]
       
       ## Sources
       1. [Source 1 description]
       2. [Source 2 description]
       3. [Source 3 description]")))


(definterface ResearchAgent
  (^String research [^String topic]))

(def agent
  (-> (AiServices/builder ResearchAgent)
      (.chatModel model)
      (.toolProvider brave-tool-provider)
      (.systemMessageProvider system-message-fn)
      (.build)))

(println
 (.research agent "Overview of Clojure concurrency"))
