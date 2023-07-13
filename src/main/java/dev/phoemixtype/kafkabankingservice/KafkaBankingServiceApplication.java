package dev.phoemixtype.kafkabankingservice;

import com.ibm.gbs.schema.Balance;
import com.ibm.gbs.schema.Customer;
import com.ibm.gbs.schema.CustomerBalance;
import dev.phoemixtype.kafkabankingservice.config.CustomerBalanceConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.JoinWindows;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@SpringBootApplication
public class KafkaBankingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(KafkaBankingServiceApplication.class, args);
	}

	@Bean
	NewTopic balance() {
		return TopicBuilder.name("Balance")
				.partitions(6)
				.replicas(3)
				.build();
	}

	@Bean
	NewTopic customer() {
		return TopicBuilder.name("Customer")
				.partitions(6)
				.replicas(3)
				.build();
	}

	@Bean
	NewTopic customerBalance() {
		return TopicBuilder.name("CustomerBalance")
				.partitions(6)
				.replicas(3)
				.build();
	}

}


@Component
class Consumer {

	@KafkaListener(topics = {"${kafka.customer.topic}"}, groupId = "kafka-cloud-consumer")
	public void consumeCustomer(ConsumerRecord<String, Customer> customerConsumerRecord) {
		System.out.println("Received for customer = " + customerConsumerRecord.value() + ", with key for customer = " + customerConsumerRecord.key());
	}

	@KafkaListener(topics = "${kafka.balance.topic}", groupId = "kafka-cloud-consumer")
	public void consumeBalance(ConsumerRecord<String, Balance> balanceConsumerRecord) {
		System.out.println("Received for balance = " + balanceConsumerRecord.value() + ", with key for balance = " + balanceConsumerRecord.key());
	}

	@KafkaListener(topics = "${kafka.customer.balance.topic}", groupId = "kafka-cloud-consumer")
	public void consumeCustomerBalance(ConsumerRecord<String, CustomerBalance> customerBalanceConsumerRecord) {
		System.out.println("Received for customer balance = " + customerBalanceConsumerRecord.value() + ", with key for customer balance = " + customerBalanceConsumerRecord.key());
	}
}

@Component
@EnableKafkaStreams
class Processor {

	@Value("${kafka.customer.topic}")
	private String customerTopic;

	@Value("${kafka.balance.topic}")
	private String balanceTopic;

	@Value("${kafka.customer.balance.topic}")
	private String customerBalanceTopic;

	@Value("${spring.kafka.properties.schema.registry.url}")
	private String schemaRegistryUrl;

	private final CustomerBalanceConfig customerBalanceConfig;

	Processor(CustomerBalanceConfig customerBalanceConfig) {
		this.customerBalanceConfig = customerBalanceConfig;
	}

	@Autowired
	public KStream<String, CustomerBalance> process(StreamsBuilder streamsBuilder) {

		final Serde<String> stringSerde = Serdes.String();
		final SpecificAvroSerde<Customer> customerSerde = new SpecificAvroSerde<>();
		final SpecificAvroSerde<Balance> balanceSerde = new SpecificAvroSerde<>();
		final SpecificAvroSerde<CustomerBalance> customerBalanceSerde = new SpecificAvroSerde<>();

		customerSerde.configure(Map.of("schema.registry.url", schemaRegistryUrl), false);
		balanceSerde.configure(Map.of("schema.registry.url", schemaRegistryUrl), false);
		customerBalanceSerde.configure(Map.of("schema.registry.url", schemaRegistryUrl), false);

		KStream<String, Customer> customerStream = streamsBuilder.stream(customerTopic, Consumed.with(stringSerde, customerSerde)).peek((key, value)
				-> { System.out.println("Customer process : " + value);
		});
		KStream<String, Balance> balanceStream = streamsBuilder.stream(balanceTopic, Consumed.with(stringSerde, balanceSerde)).peek((key, value)
				-> { System.out.println("Balance process : " + value);
		});

		KStream<String, CustomerBalance> customerBalanceKStream = streamsBuilder.stream(customerBalanceTopic, Consumed.with(stringSerde, customerBalanceSerde)).peek((key, value)
				-> { System.out.println("customer balance process : " + value);
		});

		KStream<String, CustomerBalance> joinedStream = customerStream.join(
				balanceStream,
				(customer, balance) -> {
					CustomerBalance customerBalance = new CustomerBalance();
					customerBalance.setAccountId(balance.getAccountId());
					customerBalance.setCustomerId(customer.getCustomerId());
					customerBalance.setPhoneNumber(customer.getPhoneNumber());
					customerBalance.setBalance(balance.getBalance());

					System.out.println("Joined CustomerBalance: " + customerBalance);

					return customerBalance;
				}, JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofSeconds(5))
		);

		joinedStream.foreach((key, value) -> {
			System.out.println("Joined CustomerBalance: " + value);
		});

		joinedStream.to(customerBalanceTopic, Produced.with(stringSerde, customerBalanceSerde));
		streamsBuilder.build();

		System.out.println("Joined stream = " + customerBalanceKStream);
		return joinedStream;
	}
}