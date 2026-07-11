//Lambda Runtime delivers a batch of messages to the lambda function
//Each batch of messages has two fields EventSource and EventSourceARN
//Each batch of messages also has a field called Records
//The Records is a map with multiple keys and values
//Each key is a combination of the Topic Name and the Partition Number
//One batch of messages can contain messages from multiple partitions

/*
To simplify representing a batch of Kafka messages as a list of messages
We have created a Java class called KafkaMessage under the models package
Here we are mapping the structure of an incoming Kafka event to a list of
objects of the KafkaMessage class
 */

package com.amazonaws.services.lambda.samples.events.sns;

import java.util.List;
import java.util.Map;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

public class HandlerSNS implements RequestHandler<SNSEvent, String>{
	//Gson gson = new GsonBuilder().setPrettyPrinting().create();
	String dynamoDBTableName = System.getenv("DYNAMO_DB_TABLE");
	DynamoDBUpdater ddbUpdater = new DynamoDBUpdater(dynamoDBTableName);
	boolean addToDynamoDB=true;
	ObjectMapper objectMapper = new ObjectMapper().registerModule(new JodaModule());
	@Override
	public String handleRequest(SNSEvent event, Context context)
	{
		LambdaLogger logger = context.getLogger();
		try {
			logger.log("Begin Event *************");
			try {
				logger.log(objectMapper.writeValueAsString(event));
			} catch (JsonProcessingException e1) {
				logger.log("An exception occurred " + e1.getMessage());
				throw new RuntimeException(e1);
			}
			logger.log("End Event ***************");
			List<SNSRecord> records = event.getRecords();
			for (SNSRecord record : records) {
				logger.log("Event Source = " + record.getEventSource());
				logger.log("Event Subscription ARN = " + record.getEventSubscriptionArn());
				logger.log("Event Version = " + record.getEventVersion());
				logger.log("Signing Cert URL = " + record.getSNS().getSigningCertUrl());
				logger.log("Message ID = " + record.getSNS().getMessageId());
				logger.log("Subject = " + record.getSNS().getSubject());
				logger.log("Unsubscribe URL = " + record.getSNS().getUnsubscribeUrl());
				logger.log("Signature URL = " + record.getSNS().getSignatureVersion());
				logger.log("Signature = " + record.getSNS().getSignature());
				logger.log("Type = " + record.getSNS().getType());
				logger.log("Topic ARN = " + record.getSNS().getTopicArn());
				logger.log("Timestamp = " + record.getSNS().getTimestamp().toString());
				String message = record.getSNS().getMessage();
				Person person = objectMapper.readValue(message, Person.class);
				logger.log("This person = " + person.toString());
				Map<String, SNSEvent.MessageAttribute> attributes = record.getSNS().getMessageAttributes();
				attributes.forEach((k,v) -> {
					logger.log("Attribute: " + k + ", Value: " + v.getValue());
				});
				logger.log(record.getSNS().getSignatureVersion());
				String AWS_SAM_LOCAL = System.getenv("AWS_SAM_LOCAL");
				if ((null == AWS_SAM_LOCAL) && (addToDynamoDB)) {
					ddbUpdater.insertIntoDynamoDB(record, person, logger);
				}
			}
		} catch (Exception e) {
			logger.log("An exception occurred " + e.getMessage());
			throw new RuntimeException(e);
		}
		return new String("200-OK");
	}
	
	public void throwit(String message) throws Exception{
		throw new Exception(message);
	}
}
