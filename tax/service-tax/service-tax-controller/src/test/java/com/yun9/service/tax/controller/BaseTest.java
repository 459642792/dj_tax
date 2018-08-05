package com.yun9.service.tax.controller;

import com.yun9.service.tax.TaxApplication;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-04-28 14:09
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TaxApplication.class)
@WebAppConfiguration
public abstract class BaseTest {

}
