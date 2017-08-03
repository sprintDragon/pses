package org.sprintdragon.pses.core.test;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.sprintdragon.pses.core.common.network.NetworkService;

import java.util.Collections;

/**
 * Created by wangdi on 17-8-3.
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class NetworkServiceTest {

    NetworkService networkService;

    @Before
    public void setUp() throws Exception {
        networkService = new NetworkService(Collections.emptyList());
    }

    @Test
    public void testGetIp() throws Exception {
        System.out.println("##" + networkService.resolvePublishHostAddress("0.0.0.0"));
    }
}
