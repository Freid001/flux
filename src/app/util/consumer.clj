(ns app.util.consumer
  (:require
    [cheshire.core :as json]
    [kafka-avro-confluent.schema-registry-client :as reg]
    [clojure.core.async :as a :refer [>! <! >!! <!! go chan buffer close! thread alts! alts!! timeout]]
    [abracad.avro :as avro]
    [clojure.tools.logging :as logger]
    [kafka-avro-confluent.magic :as magic]
    [slingshot.slingshot :refer :all])
  (:import org.apache.kafka.clients.producer.ProducerRecord
           org.apache.kafka.clients.producer.KafkaProducer
           org.apache.kafka.clients.consumer.KafkaConsumer
           java.nio.ByteBuffer
           org.apache.kafka.common.serialization.ByteArraySerializer
           org.apache.kafka.common.serialization.ByteArrayDeserializer))

(def ^:private c-cfg
  {"bootstrap.servers"  "kafka1:9092"
   "group.id"           "consumer2"
   "auto.offset.reset"  "earliest"
   "enable.auto.commit" "true"
   "key.deserializer"   ByteArrayDeserializer
   "value.deserializer" ByteArrayDeserializer})

(def ^:private get-schema-by-id-memo (memoize reg/get-avro-schema-by-id))
(def ^:private schema-registry (reg/->schema-registry-client {:base-url "http://localhost:8081"}))
(def ^:private consumer (doto (KafkaConsumer. c-cfg) (.subscribe ["avro-java"])))

(defn- byte-buffer->bytes
  [buffer]

  (let [array (byte-array (.remaining buffer))]
    (.get buffer array)
    array))

(defn- read-record
  "Read Kafka record."
  [record]

  (try
    (let [buffer (ByteBuffer/wrap record)
          schema (get-schema-by-id-memo schema-registry (.getInt buffer))]

      (if (= magic/magic (.get buffer))
        (avro/decode schema (byte-array (byte-buffer->bytes buffer)))
        (throw (Exception. "Invalid magic byte!"))))

    (catch Exception e (logger/log :error e) nil)))

(defn poll
  "Poll Kafka topic."
  [fn]

  (if (fn? fn)
    (while true
      (let [records (.poll consumer 100)]
        (doseq [record records]
          (fn (read-record (.value record))))))))