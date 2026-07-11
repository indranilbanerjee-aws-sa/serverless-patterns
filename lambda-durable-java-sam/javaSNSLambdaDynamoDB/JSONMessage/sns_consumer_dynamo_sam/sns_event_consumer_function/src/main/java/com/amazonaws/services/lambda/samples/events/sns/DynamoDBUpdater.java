package com.amazonaws.services.lambda.samples.events.sns;


import java.util.Map;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;

public class DynamoDBUpdater {

	String dynamoDBTableName;
	AmazonDynamoDB client;
	DynamoDB dynamoDB;
	Table dynamoTable;
	

	public DynamoDBUpdater(String dynamoDBTableName) {
		super();
		if (null == dynamoDBTableName) {
			this.dynamoDBTableName = "SNS_LAMBDA_DYNAMO_TABLE";
		} else {
			this.dynamoDBTableName = dynamoDBTableName;
		}
		String AWS_SAM_LOCAL = System.getenv("AWS_SAM_LOCAL");
		if (null == AWS_SAM_LOCAL) {
			this.client = AmazonDynamoDBClientBuilder.standard().build();
		} else {
			this.client = AmazonDynamoDBClientBuilder.standard().withEndpointConfiguration(new EndpointConfiguration("http://127.0.0.1:8000", "")).build();
			this.dynamoDBTableName = "SQS_LAMBDA_DYNAMO_TABLE";
		}
		//this.client = AmazonDynamoDBClientBuilder.standard().build();
		this.dynamoDB = new DynamoDB(client);
		this.dynamoTable = dynamoDB.getTable(this.dynamoDBTableName);
	}
	
	public PutItemOutcome insertIntoDynamoDB(SNSRecord record, Person thisPerson, LambdaLogger logger) {
		logger.log("Now inserting a row in DynamoDB for messageID = " + record.getSNS().getMessageId());
		Item item = new Item();
		item.withPrimaryKey("MessageID", record.getSNS().getMessageId());
		item.withString("EventSource", record.getEventSource());
		item.withString("EventSubscriptionARN", record.getEventSubscriptionArn());
		item.withString("EventVersion", record.getEventVersion());
		item.withString("SigningCertURL", record.getSNS().getSigningCertUrl());
		item.withString("MessageID", record.getSNS().getMessageId());
		item.withString("Subject", record.getSNS().getSubject());
		item.withString("UnsubscribeURL", record.getSNS().getUnsubscribeUrl());
		item.withString("SignatureURL", record.getSNS().getSignatureVersion());
		item.withString("Signature", record.getSNS().getSignature());
		item.withString("Type", record.getSNS().getType());
		item.withString("TopicARN", record.getSNS().getTopicArn());
		item.withString("Timestamp", record.getSNS().getTimestamp().toString());
		item.withString("Firstname", thisPerson.getFirstname());
		item.withString("Lastname", thisPerson.getLastname());
		item.withString("Company", thisPerson.getCompany());
		item.withString("Address_Street", thisPerson.getStreet());
		item.withString("City", thisPerson.getCity());
		item.withString("County", thisPerson.getCounty());
		item.withString("State", thisPerson.getState());
		item.withString("Zip", thisPerson.getZip());
		item.withString("Home_Phone", thisPerson.getHomePhone());
		item.withString("Cell_Phone", thisPerson.getCellPhone());
		item.withString("Email", thisPerson.getEmail());
		item.withString("Website", thisPerson.getWebsite());
		Map<String, SNSEvent.MessageAttribute> attributes = record.getSNS().getMessageAttributes();
		attributes.forEach((k,v) -> {
			item.withString(k, v.getValue());
		});
	    logger.log("Now done inserting a row in DynamoDB for messageID = " + record.getSNS().getMessageId());
		return dynamoTable.putItem(item);
	}
	
}
