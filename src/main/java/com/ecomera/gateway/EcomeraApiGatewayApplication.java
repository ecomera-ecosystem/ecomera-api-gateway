package com.ecomera.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class EcomeraApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcomeraApiGatewayApplication.class, args);
	}

}
