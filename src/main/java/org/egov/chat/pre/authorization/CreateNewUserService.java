package org.egov.chat.pre.authorization;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CreateNewUserService {

    @Autowired
    private RestTemplate restTemplate;



}
