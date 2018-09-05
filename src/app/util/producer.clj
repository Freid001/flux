(ns app.util.producer
  (:require [kafka-avro-confluent.schema-registry-client :as reg]
            [clojure.core.async :as a :refer [>! <! >!! <!! go chan buffer close! thread alts! alts!! timeout]]
            [abracad.avro :as avro]
            [cheshire.core :as json]
            [clojure.tools.logging :as logger]
            [kafka-avro-confluent.magic :as magic]
            [slingshot.slingshot :refer :all])
  (:import org.apache.kafka.clients.producer.ProducerRecord
           org.apache.kafka.clients.producer.KafkaProducer
           org.apache.kafka.common.serialization.ByteArraySerializer
           java.nio.ByteBuffer
           java.io.ByteArrayOutputStream))

(def ^:private channel (chan))

(def ^:private p-cfg
  {"bootstrap.servers" "kafka1:9092"
   "key.serializer"    ByteArraySerializer
   "value.serializer"  ByteArraySerializer})

(def ^:private producer (KafkaProducer. p-cfg))
(def ^:private schema-registry (reg/->schema-registry-client {:base-url "http://localhost:8081"}))
(def ^:private post-schema-memo (memoize reg/post-schema))

(defn- schema-id->bytes
  [schema-id]

  (-> (ByteBuffer/allocate 4)
      (.putInt schema-id)
      .array))

(defn- ->serialized-bytes
  [schema-id avro-schema data]

  (with-open [out (ByteArrayOutputStream.)]
    (.write out magic/magic)
    (.write out (schema-id->bytes schema-id))
    (.write out (avro/binary-encoded avro-schema data)) ;fails if not valid avro schema.
    (.toByteArray out)))

(defn- serialize
  [schema-registry serializer-type topic schema data]

  (when data
    (let [avro-schema (avro/parse-schema schema)
          subject (format "%s-%s" topic (name serializer-type))
          schema-id (post-schema-memo schema-registry subject schema)
          serialized-bytes (->serialized-bytes schema-id avro-schema data)]
      serialized-bytes)))

(defn write
  [topic schema-name record]

  (let [schema (json/parse-string (:schema (reg/get-latest-schema-by-subject schema-registry schema-name)) true)]
    (try
      (go (>! channel (.send producer (ProducerRecord. topic (serialize schema-registry "value" topic schema record)))))
      (catch Exception e (logger/log :error e)))))