package com.telas.services.impl;

import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import com.telas.entities.Client;
import com.telas.repositories.ClientRepository;
import com.telas.repositories.SubscriptionRepository;
import com.telas.services.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {
    private final SubscriptionRepository repository;
    private final ClientRepository clientRepository;

    private void createStripeCustomer(Client client) throws StripeException {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(client.getContact().getEmail())
                .setName(client.getBusinessName())
                .build();

        Customer customer = Customer.create(params);
        client.setStripeCustomerId(customer.getId());
        clientRepository.save(client);
    }
}
