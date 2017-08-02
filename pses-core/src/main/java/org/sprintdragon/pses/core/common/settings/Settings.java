package org.sprintdragon.pses.core.common.settings;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;

/**
 * Created by wangdi on 17-8-2.
 */
@Component
public class Settings extends HashMap<String, String> {

    @PostConstruct
    public void init() throws Exception {
        put("bind_host", "0.0.0.0");
        put("port", "9090");
    }

}
