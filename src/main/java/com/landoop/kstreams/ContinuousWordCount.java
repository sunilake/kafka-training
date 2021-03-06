package com.landoop.kstreams;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.KTable;

import java.util.Arrays;
import java.util.Properties;

public class ContinuousWordCount {

  public static void main(String[] args) throws Exception {

    String brokers = System.getenv("BROKERS");

    Properties props = new Properties();
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount");
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
    props.put(StreamsConfig.KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
    props.put(StreamsConfig.VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

    // setting offset reset to earliest so that we can re-run the demo code with the same pre-loaded data
    // Note: To re-run the demo, you need to use the offset reset tool:
    // https://cwiki.apache.org/confluence/display/KAFKA/Kafka+Streams+Application+Reset+Tool
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");


    // Construct a `KStream` from the input topic ""streams-file-input", where message values
    // represent lines of text (for the sake of this example, we ignore whatever may be stored
    // in the message keys).
    KStreamBuilder builder = new KStreamBuilder();
    KStream<String, String> textLines = builder.stream("quotes");

    KTable<String, String> count = textLines
            // Split each text line, by whitespace, into words.
            // NO RE-PARTITIONING AS WE DON'T CHANGE THEY KEY
            .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
            // Group the text words as message keys
            .groupBy((key, word) -> word)
            // Count the occurrences of each word (message key).
            .count("WordCount")
            .mapValues(value -> value.toString());

    count.print();
    // Store the running counts as a changelog stream to the output topic.
    count.to(Serdes.String(), Serdes.String(), "quotes-wordcount");

    KafkaStreams streams = new KafkaStreams(builder, props);
    streams.start();

    // the stream application will be running forever
  }

}
