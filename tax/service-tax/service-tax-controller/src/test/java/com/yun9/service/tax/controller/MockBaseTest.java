package com.yun9.service.tax.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yun9.service.tax.TaxApplication;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-04-28 14:09
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TaxApplication.class)
@WebAppConfiguration
public abstract class MockBaseTest {

    protected ObjectMapper mapper = new ObjectMapper();
    protected MockMvc mvc;

    @Before
    public void before() throws Exception {
        mvc = MockMvcBuilders.standaloneSetup(controller()).build();
    }

    public abstract Object controller();
}